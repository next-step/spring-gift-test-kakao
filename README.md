# spring-gift-test

카카오 스타일 선물하기 시스템 — Spring Boot 백엔드 API

## 사전 준비

| 필수 항목 | 버전 |
|-----------|------|
| **Java** | 21 (Gradle toolchain이 자동 감지) |
| **Docker** | Docker Desktop 또는 Colima |

> Gradle Wrapper(`./gradlew`)가 포함되어 있어 별도 Gradle 설치는 불필요합니다.

### macOS + Colima 사용 시

Testcontainers가 Docker 소켓을 찾을 수 있도록 설정이 필요합니다.

```bash
# 1. Colima 시작
colima start

# 2. Testcontainers 설정 파일 생성
cat > ~/.testcontainers.properties << 'EOF'
docker.host=unix:///Users/${USER}/.colima/default/docker.sock
EOF
```

> `build.gradle`에 Colima 소켓 자동 감지가 내장되어 있어, 대부분의 경우 위 설정만으로 동작합니다.

---

## 테스트 실행

### 한눈에 보기

```
./gradlew test              ← JUnit 테스트 (단위 + 인수)     17개
./gradlew cucumberTest      ← Cucumber BDD 테스트             7개
./gradlew test cucumberTest ← 전체 테스트                     24개
```

---

### 1. JUnit 테스트 (단위 + 인수)

Testcontainers가 PostgreSQL을 자동으로 띄우므로 별도 DB 설치가 불필요합니다.

```bash
# 전체 JUnit 테스트
./gradlew test

# 인수 테스트만 (Spring Boot 통합 테스트)
./gradlew test --tests "gift.GiftAcceptanceTest"

# 단위 테스트만 (Spring 컨텍스트 없음)
./gradlew test --tests "gift.model.OptionTest"
```

**동작 방식:**
1. Testcontainers가 `postgres:16-alpine` 컨테이너를 자동 실행
2. `@ServiceConnection`으로 DB 연결 자동 구성
3. 각 테스트 전 `TRUNCATE CASCADE`로 데이터 격리
4. 테스트 종료 후 컨테이너 자동 정리

**테스트 결과 확인:**
```bash
open build/reports/tests/test/index.html
```

---

### 2. Cucumber BDD 테스트

Docker Compose로 앱 전체를 컨테이너화하여 E2E 테스트를 실행합니다.

```bash
./gradlew cucumberTest
```

**동작 방식:**
1. `./gradlew dockerBuild` → Docker 이미지 빌드
2. `docker compose up` → PostgreSQL(`:15432`) + App(`:28080`) 컨테이너 시작
3. RestAssured로 `http://localhost:28080`에 HTTP 요청 → Korean Gherkin 시나리오 실행
4. 테스트 종료 후 `docker compose down -v`로 자동 정리

> `dockerUp` → `cucumberTest` → `dockerDown` 이 자동으로 체이닝됩니다.

---

### 3. 전체 테스트

```bash
./gradlew test cucumberTest
```

JUnit 17개 + Cucumber 7개 = **총 24개** 테스트가 실행됩니다.

---

## Docker 명령어 (수동 실행)

개별적으로 Docker 환경을 제어할 때 사용합니다.

```bash
./gradlew dockerBuild   # Docker 이미지 빌드
./gradlew dockerUp      # 컨테이너 시작 (postgres + app)
./gradlew dockerDown    # 컨테이너 중지 + 볼륨 제거
```

수동으로 앱이 떠 있는 상태에서 API를 직접 호출해볼 수 있습니다:

```bash
# 컨테이너 시작
./gradlew dockerUp

# API 호출 예시
curl -X POST http://localhost:28080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name": "간식"}'

# 정리
./gradlew dockerDown
```

---

## 빌드

```bash
./gradlew build    # 컴파일 + 테스트 + JAR 생성
```

빌드 산출물: `build/libs/*.jar`

---

## 트러블슈팅

| 증상 | 해결 |
|------|------|
| `Could not find a valid Docker environment` | Docker Desktop 또는 Colima가 실행 중인지 확인 |
| `Connection refused (localhost:15432)` | `./gradlew dockerDown` 후 재시도 — 이전 컨테이너가 남아 있을 수 있음 |
| Colima 소켓 에러 | `~/.testcontainers.properties`에 소켓 경로가 올바른지 확인 |
| `port is already allocated` | `docker compose down -v` 로 기존 컨테이너 정리 |
| 테스트 격리 실패 (데이터 오염) | `TRUNCATE CASCADE`가 `@BeforeEach`에서 실행되는지 확인 |
