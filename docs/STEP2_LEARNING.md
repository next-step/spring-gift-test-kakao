# Step 1: Cucumber BDD 학습 기록

## Cucumber란?

테스트 시나리오를 비즈니스 언어(한글)로 작성하고, 그 문장과 매칭되는 코드를 실행하는 BDD 프레임워크.

기존 RestAssured 테스트는 시나리오와 실행 로직이 한 메서드에 섞여 있었는데,
Cucumber는 이를 **"무엇을 테스트하는가"(feature 파일)**와 **"어떻게 실행하는가"(Step Definition)**로 분리한다.

## 실행 흐름

```
1. CucumberSuite가 JUnit에 의해 실행됨
2. features/ 디렉터리에서 .feature 파일 스캔
3. glue 패키지에서 Cucumber 어노테이션이 붙은 메서드 수집
4. feature 파일의 각 줄 텍스트 ↔ 어노테이션 텍스트를 패턴 매칭
5. 매칭된 메서드를 순서대로 실행
```

## CucumberSuite 어노테이션 역할

```java
@Suite // JUnit에게 테스트 묶음임을 알림
@IncludeEngines("cucumber") // Cucumber 엔진으로 실행
@SelectClasspathResource("features") // feature 파일 디렉터리 지정
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.acceptance.cucumber") // Step Definition 스캔 패키지 지정
```

## Feature 파일 (Gherkin)

- `# language: ko`로 한국어 키워드 활성화
- 확장자가 `.feature`이면 되고 파일명은 자유
- 시나리오 단위로 테스트가 실행됨

### 한글 키워드

| 한국어 | 영어 | 역할 |
|--------|------|------|
| 기능 | Feature | 기능 묶음 |
| 시나리오 | Scenario | 개별 테스트 케이스 |
| 먼저 | Given | 전제 조건 |
| 만일 | When | 실행할 행위 |
| 그러면 | Then | 기대 결과 |
| 그리고 | And | 이전 키워드에 추가 |

이 키워드들은 Cucumber가 언어별로 사전 정의한 예약어로, 변경할 수 없다.

### 키워드 매칭 규칙

Cucumber는 `먼저/만일/그러면/그리고`를 **구분하지 않는다.**
어노테이션이 `@그리고`로 되어 있어도 feature에서 `그러면`으로 쓸 수 있다.
키워드는 사람이 읽을 때 자연스러운 문장을 위한 것이고, Cucumber는 **키워드 뒤의 텍스트만** 매칭한다.

## Step Definition

- 클래스명, 메서드명은 자유. Cucumber는 어노테이션 텍스트로만 매칭한다.
- `{int}`, `{string}` 같은 플레이스홀더가 메서드 파라미터로 추출된다.
- glue 패키지 안의 클래스를 스캔해서 Cucumber 어노테이션이 붙은 메서드만 수집한다.
- 관련 없는 클래스나 메서드는 무시된다.

### 빈 등록 방식

- Step Definition 클래스: Cucumber-Spring이 glue 패키지 스캔 시 자동으로 빈 등록. `@Component` 불필요.
- SharedContext: `@ScenarioScope`(Spring 빈 스코프 기능)를 쓰므로 `@Component` 필요.
- CucumberSpringConfig: `@CucumberContextConfiguration`이 glue 패키지 안에 있어야 Cucumber-Spring이 인식.

## `@Before` 훅

- Cucumber의 `@Before`는 **모든 시나리오**에 대해 실행된다. 클래스 단위가 아니다.
- glue 패키지 어디에 있든 상관없이 모든 시나리오 실행 전에 호출된다.
- 특정 시나리오에만 실행하려면 태그를 사용한다: `@Before("@태그명")`

## 데이터 준비 전략

### 시나리오별 최적화된 데이터셋

하나의 공통 데이터셋("테스트 데이터가 준비되어 있다")보다 시나리오 목적에 맞는 데이터만 준비하는 것이 좋다:

```gherkin
# 선물: 회원, 카테고리, 상품, 옵션 전부 필요
먼저 id 1번의 "보내는사람" 회원이 존재한다
그리고 id 100번의 "테스트카테고리" 카테고리가 존재한다
그리고 ...

# 카테고리 조회: 카테고리만 필요
먼저 id 100번의 "테스트카테고리" 카테고리가 존재한다
```

이렇게 하면 feature 파일만 보고 각 시나리오의 전제 조건을 바로 파악할 수 있다.

### ID 직접 지정 vs 이름 기반 조회

setup step에서 ID를 직접 지정하면 action/verification step에서 SELECT 쿼리 없이 바로 ID를 사용할 수 있다:

```java
// ID 직접 지정 - SELECT 불필요
@먼저("id {int}번의 {string} 카테고리가 존재한다")
public void 카테고리가_존재한다(int id, String name) {
    jdbcTemplate.update("INSERT INTO category (id, name) VALUES (?, ?)", id, name);
}
```

## 파일 구조

```
src/test/
├── java/gift/acceptance/
│   ├── CucumberSuite.java              # 테스트 실행 진입점
│   ├── CategoryAcceptanceTest.java     # 기존 RestAssured 테스트 (유지)
│   ├── ProductAcceptanceTest.java
│   ├── GiftAcceptanceTest.java
│   └── cucumber/
│       ├── CucumberSpringConfig.java   # Spring Boot 연동 설정
│       ├── SharedContext.java          # 시나리오 내 Response 공유
│       ├── CommonStepDefs.java         # 공통 step (데이터 준비, 응답 검증)
│       ├── CategoryStepDefs.java       # 카테고리 도메인 step
│       ├── ProductStepDefs.java        # 상품 도메인 step
│       └── GiftStepDefs.java           # 선물 도메인 step
└── resources/
    └── features/
        ├── category.feature            # 카테고리 시나리오
        ├── product.feature             # 상품 시나리오
        └── gift.feature                # 선물 시나리오
```

### StepDefs 분리 기준

도메인별로 나누되, 여러 도메인에서 공유되는 step(데이터 준비, 응답 검증)은 CommonStepDefs에 둔다.
데이터 준비 step은 카테고리 insert가 상품 feature에서도 쓰이는 등 도메인을 넘나들기 때문에 Common이 적절하다.

---

# Step 2: PostgreSQL + Docker Compose 학습 기록

## Production Parity

테스트 DB와 운영 DB가 다르면(H2 vs PostgreSQL) DB별 문법 차이로 테스트에서 잡히지 않는 버그가 생길 수 있다.
Cucumber 테스트를 PostgreSQL로 전환하여 운영 환경과 동일한 DB로 검증한다.

## Docker Compose

여러 컨테이너를 `docker-compose.yml` 파일 하나로 정의하고 관리하는 도구.

- `docker-compose.yml`은 기본 파일명. 다른 이름이면 `-f` 옵션 필요.
- `services`, `volumes`, `healthcheck` 등은 Docker Compose 스펙에 정의된 예약 키.
- 테스트용 로컬 컨테이너에서는 DB 비밀번호를 직접 노출해도 괜찮지만, 운영 환경에서는 `.env` 파일로 분리한다.

### 주요 명령어 옵션

- `-d` (detached): 컨테이너를 백그라운드에서 실행. 없으면 터미널이 컨테이너 로그에 점유됨.
- `--wait`: healthcheck가 healthy가 될 때까지 대기. 최초 1회 healthy 시점에 명령 완료.
- `-v` (`down -v`): 컨테이너와 함께 볼륨도 삭제하여 깨끗하게 정리.

### Healthcheck

컨테이너 시작 직후부터 `interval` 간격으로 반복 실행되는 상태 확인.
`pg_isready`는 PostgreSQL이 제공하는 유틸리티로, 연결을 받을 준비가 됐는지 확인한다(exit code 0 = 준비 완료).
`--wait`는 이 healthcheck가 최초 성공할 때까지 기다린 후 완료된다.
healthcheck가 없으면 `--wait`는 컨테이너 시작만으로 완료되어, DB가 아직 준비 안 된 상태에서 테스트가 시작될 수 있다.

## Gradle Exec task

외부 명령어(프로세스)를 실행하는 Gradle 내장 task 타입.
`commandLine`에 지정된 명령어 실행 자체가 본 동작이므로 `doFirst`가 필요 없다.

### task 연결

- `dependsOn`: 이 task 실행 전에 다른 task를 먼저 실행 (task 간 실행 순서)
- `finalizedBy`: 이 task의 성공/실패와 관계없이 항상 지정된 task를 실행 (정리 작업에 사용)
- `doFirst`: 하나의 task 내부에서 본 동작 직전에 실행할 코드 블록 추가 (task 내부 훅)

```
dockerUp (dependsOn)  →  cucumberTest  →  dockerDown (finalizedBy, 실패해도 실행)
```

컨테이너 시작/종료는 task 단위로 1번씩만 실행된다. 매 시나리오마다 실행되지 않는다.
시나리오별 데이터 정리는 `@Before`의 `TRUNCATE`가 담당한다.

## JUnit Platform 엔진 필터링

Cucumber 엔진은 Suite 엔진과 독립적으로 테스트를 발견한다.
`excludeTags`로는 Cucumber 엔진의 직접 발견을 막을 수 없어서 `excludeEngines`로 엔진 자체를 제외해야 한다.

```groovy
// 기존 테스트: Cucumber 엔진 제외 + CucumberSuite 클래스 제외
tasks.named('test') {
    useJUnitPlatform { excludeEngines 'cucumber' }
    exclude '**/CucumberSuite*'
}

// Cucumber 테스트: Cucumber 엔진만 포함
tasks.register('cucumberTest', Test) {
    useJUnitPlatform { includeEngines 'cucumber' }
}
```

Cucumber 엔진이 직접 실행될 때 glue 경로는 `systemProperty`로 설정할 수 있다.
`cucumber.glue`를 지정하지 않으면 루트 패키지부터 전체 스캔하므로 동작은 하지만 명시적으로 지정하는 것이 낫다.

## Spring Profile

활성화된 프로필 이름에 따라 `application-{프로필명}.properties`를 추가 로드하는 메커니즘.

- 클래스 로딩 시점이 아니라 **Spring ApplicationContext 초기화 시점**에 처리된다.
- 기본 `application.properties`를 먼저 로드하고, 프로필 설정이 같은 키를 덮어쓴다.
- 프로필이 다르면 Spring이 별도의 ApplicationContext를 생성한다.
- 여러 프로필을 동시에 활성화할 수 있고, 뒤에 지정된 프로필이 우선순위가 높다.
- 테스트 전용이 아니라 애플리케이션 전체에서 동작한다 (dev/prod 분리 등).

```java
// 단일 프로필
@ActiveProfiles("cucumber")

// 복수 프로필 (뒤가 우선순위 높음)
@ActiveProfiles({"test", "cucumber"})
```

## 테스트 분리 구조

| 명령 | 실행 대상 | DB | 실행 환경 |
|------|----------|-----|-----------|
| `./gradlew test` | RestAssured 인수 테스트 3개 | H2 | 호스트 |
| `./gradlew cucumberTest` | Cucumber 시나리오 8개 | PostgreSQL (Docker) | 호스트에서 실행, DB만 컨테이너 |

테스트 코드는 호스트(로컬 JVM)에서 실행되고, PostgreSQL만 Docker 컨테이너에서 실행된다.
`ports: "5432:5432"`가 호스트와 컨테이너 포트를 연결한다.

---

# Step 3: Application 컨테이너화 학습 기록

## 왜 애플리케이션까지 컨테이너로 실행하는가?

Step 2에서는 DB만 컨테이너였고 앱은 로컬 JVM에서 실행되었다.
로컬 JDK 버전, OS 차이 등으로 "내 PC에서는 되는데 서버에서는 안 되는" 문제가 발생할 수 있다.
앱까지 컨테이너화하면 프로덕션과 완전히 동일한 환경에서 테스트할 수 있다.

## 아키텍처

```
테스트(Host JVM)  --HTTP-->  localhost:28080 (Docker App 컨테이너)
테스트(Host JVM)  --JDBC-->  localhost:5432  (Docker DB 컨테이너)
App(Container)  --JDBC-->  postgres:5432   (Docker DB 컨테이너, 내부 네트워크)
```

테스트 코드는 로컬에서 실행되고, 앱과 DB는 Docker 컨테이너에서 실행된다.
앱 컨테이너 내부에서는 서비스 이름(`postgres`)이 hostname으로 동작한다.

## Docker 핵심 개념

### 이미지 / 서비스 / 컨테이너

| 비유 | Docker | 설명 |
|------|--------|------|
| 소스코드 | 이미지 | 실행에 필요한 파일 묶음 |
| 클래스 | 서비스 | 이미지 + 실행 설정 (포트, 환경변수 등) |
| 인스턴스 | 컨테이너 | 서비스를 실제로 실행한 것 |

하나의 서비스에서 `--scale`로 여러 컨테이너를 띄울 수 있다. 보통은 1:1로 사용한다.
만약 여러개 띄우고 싶다면 포트 고정은 해제해야 한다.

### Docker 내부 네트워크

Docker Compose가 자동으로 내부 네트워크를 만들고, 각 서비스 이름을 DNS에 등록한다.
서비스 이름이 곧 컨테이너 간 통신의 hostname이 된다.

## Multi-stage build

Dockerfile에서 `FROM`으로 시작하는 각 단계를 스테이지라 한다. 마지막 스테이지만 최종 이미지가 되고, 이전 스테이지는 버려진다.
`COPY --from=builder`로 이전 스테이지의 결과물만 가져올 수 있다.

빌드에는 JDK + Gradle + 소스코드가 필요하지만 실행에는 JRE + JAR만 있으면 된다.
스테이지를 나눠서 최종 이미지에 빌드 도구를 포함하지 않아 이미지 크기를 줄인다.

`AS builder`는 스테이지 별칭. 번호(`--from=0`)로도 참조 가능하지만 별칭이 가독성이 좋다.

### 이미지 태그

- `eclipse-temurin:21-jdk` — Debian + JDK (빌드용, 가장 무거움)
- `eclipse-temurin:21-jre` — Debian + JRE (실행용)
- `eclipse-temurin:21-jre-alpine` — Alpine + JRE (실행용, 가장 가벼움)

`alpine`은 경량 리눅스 배포판. 단, `jdk-alpine`은 Gradle 네이티브 라이브러리와 호환 문제(musl libc)가 있어 빌드 스테이지에는 Debian 기반을 사용했다.

## depends_on과 --wait의 차이

- `depends_on: condition: service_healthy` → 컨테이너 간 시작 순서 제어 (postgres healthy → app 시작)
- `--wait` → `docker-compose up` 명령이 모든 서비스가 healthy될 때까지 대기 후 리턴

둘은 별개. `depends_on`은 컨테이너 간 순서, `--wait`는 CLI 명령의 리턴 시점을 제어한다.

### healthcheck가 없는 서비스

- `--wait`: 체크를 건너뛰고 시작만으로 ready 처리
- `depends_on: condition: service_healthy`: 에러 발생. `condition: service_started`를 써야 함

## .dockerignore

`.gitignore`의 Docker 버전. `COPY . .` 시 불필요한 파일 전송을 방지한다.
빌드 속도 저하뿐 아니라 `.git/` 등이 포함되면 Docker 레이어 캐시가 불필요하게 무효화된다.

## webEnvironment = NONE

앱이 Docker 컨테이너에서 이미 실행되므로 테스트 JVM에서 앱을 또 띄울 필요가 없다.
`NONE`이면 임베디드 웹서버를 시작하지 않지만, Spring 컨텍스트는 로드된다 (JdbcTemplate으로 DB cleanup 필요).

## JdbcTemplate이 필요한 이유

앱이 Docker 컨테이너 안에 있으므로 로컬에서 앱의 Repository나 Service를 직접 호출할 수 없다.
DB는 `localhost:5432`로 직접 접근 가능하므로 JdbcTemplate으로 TRUNCATE하여 데이터를 정리한다.

## dockerBuild와 Docker 레이어 캐시

`docker-compose up`은 이미지가 이미 존재하면 재빌드하지 않는다.
`dockerBuild`(`docker-compose build`)는 항상 빌드를 시도하지만, 코드 변경이 없으면 Docker 레이어 캐시 덕분에 빠르게 완료된다.
`dockerUp`이 `dockerBuild`에 의존하도록 설정하여 항상 최신 이미지로 테스트하게 했다.

## cucumber.features 시스템 프로퍼티

`cucumberTest`에서 `includeEngines 'cucumber'`로 cucumber 엔진을 직접 사용할 때, feature 파일 위치를 명시적으로 지정해야 테스트가 발견된다.
`CucumberSuite`의 `@SelectClasspathResource`는 `junit-platform-suite` 엔진용이므로 별개.

## 테스트 분리 구조 (최종)

| 명령 | 실행 대상 | DB | 앱 실행 위치 |
|------|----------|-----|-------------|
| `./gradlew test` | RestAssured 인수 테스트 3개 | H2 | 로컬 JVM |
| `./gradlew cucumberTest` | Cucumber 시나리오 8개 | PostgreSQL (Docker) | Docker 컨테이너 |
