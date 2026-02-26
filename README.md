# spring-gift-test

카카오 선물하기 시스템의 인수 테스트 프로젝트입니다.

## 기술 스택

- Java 21, Spring Boot 3.5.8, Spring Data JPA
- Test: Cucumber 7 + JUnit 5, RestAssured
- DB: PostgreSQL (Docker Compose), H2 (인수 테스트 로컬)
- Infra: Docker, Docker Compose

## 실행 방법

### 애플리케이션 실행

```bash
./gradlew bootRun
```

### 인수 테스트 (H2 + in-process)

```bash
./gradlew test
```

### Cucumber 인수 테스트 (Docker E2E)

Docker 이미지 빌드 → 컨테이너 시작 → 테스트 실행 → 컨테이너 종료가 자동으로 수행됩니다.

```bash
./gradlew cucumberTest
```

### 수동 Docker 실행

```bash
./gradlew dockerBuild       # Docker 이미지 빌드
./gradlew dockerUp          # PostgreSQL + Application 컨테이너 시작
curl http://localhost:28080/api/categories  # 애플리케이션 응답 확인
./gradlew dockerDown        # 전체 컨테이너 종료
```
