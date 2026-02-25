# 요구사항 1: Cucumber BDD

## Gherkin

- Cucumber가 읽는 자연어 문법으로 `*.feature` 파일에 작성한다.
- `Given` / `When` / `Then` 키워드로 시나리오를 구조화한다.
- 비개발자도 읽을 수 있는 명세서 역할을 한다.
- `Feature` 는 하나의 기능단위 (=테스트 클래스), `Scenario` 는 그 기능의 구체적인 사용 사례 (=`@Test`)이다.

### Feature / Scenario

- `Feature` : 테스트 대상 기능의 제목과 설명을 작성한다.
    - 하나의 `*.feature`  파일에 하나의 `Feature` 를 작성한다.
- `Scenario` : `Feature` 안에 있는 개별 테스트 케이스이다.
    - `Given` / `When` / `Then`으로 구성된다.
    - `Scenario Outline + Examples` : 같은 흐름을 데이터만 바꿔서 반복 실행할 때 사용한다.

## Step Definition

- `*.feature` 파일의 각 `Given` / `When` / `Then` 문장을 실제 Java 코드에 매핑하는 매서드이다.
- `@Given` , `@When` , `@Then` 어노테이션으로 Gherkin 문장과 연결한다.
- 정규식이나 `Cucumber Expression`으로 파라미터를 추출할 수 있다.
- 하나의 Step 문장은 정확히 하나의 `Step Definition`에 매칭되어야 한다.

## Glue

- Cucumber가 Step Definition 클래스를 탐색할 패키지 경로다.
- `--glue gift`라고 지정하면 해당 패키지 하위의 `@Given` / `@When` / `@Then` 메서드와 Spring 설정(`@CucumberContextConfiguration`)을 모두 스캔한다.
- glue가 잘못되면 `Step`이 “undefined”로 뜬다.

## CucumberContextConfiguration

- `Cucumber`가 `Spring ApplicationContext`를 부트스트랩하는 진입점이다.
- `@CucumberContextConfiguration` + `@SpringBootTest`를 함께 붙인다.
- `Cucumber` 시나리오 전체에서 하나의 `Spring Context`를 공유한다.

## webEnvironment = NONE vs RANDOM_PORT

- `RANDOM_PORT` : 테스트 JVM 안에서 내장 톰캣을 띄운다.
    - 포트는 `@LocalServerPort`로 주입받는다.
    - 앱이 테스트 프로세스 안에서 실행된다.
- `NONE` : 웹 서버를 아예 띄우지 않는다.
    - 외부에서 이미 실행 중인 앱(Docker 컨테이너 등)에 HTTP 요청을 보내는 구조일 때 사용한다.

## Hooks(@Before / @After)

- 각 `Scenario` 실행 전후에 자동으로 호출되는 메서드다.
- 주로 DB 초기화(`TRUNCATE`), 공통 상태 세팅에 사용한다.
- JUnit의 `@BeforeEach`와 비슷하지만 `Cucumber` 생명주기에 바인딩된다.

# 요구사항 2: PostgreSQL + Docker Compost 전환

## Production Parity

- `Twelve-Factor App` 원칙 중 하나로, `개발` / `테스트` / `운영` 환경의 차이를 최소화하라는 원칙이다.
- 다른 엔진 위에서 검증하여 운영 과정에서 깨지지 않도록 도와준다.

## Docker Compose

### services

- 하나의 `servcie` = 하나의 `컨테이너`(프로세스)다.
- 서비스 이름이 곧 Docker 내부 네트워크에서의 호스트명이 된다.

### ports

- “`호스트포트:컨테이너포트`” 형식으로 매핑한다.
    - “`28080:8080`” → 호스트에서 28080으로 접근하면 컨테이너의 8080으로 전달된다.
    - 포트를 열지 않으면 외부(호스트)에서 접근할 수 없다.
    - 컨테이너끼리는 포트 매핑 없이도 내부 네트워크로 통신 가능하다.

### volumes

- 컨테이너는 삭제되면 데이터도 사라진다.
- `volume`은 데이터를 컨테이너 밖에 보존하는 장치다.
- `gift-test-data:/var/lib/postgresql/data` → PostgreSQL 데이터가 named volume에 저장된다.
- 테스트 환경에서는 데이터 보존보다 깨끗한 상태 유지가 더 중요할 수 있다.
    - `docker-compose down -v` : volume 까지 삭제

### healthcheck

- 컨테이너가 “running”이어도 앱이 준비된 건 아니다.
- PostgreSQL 프로세스가 떠도 TCP 연결을 받을 준비가 안 되었을 수 있다.
- `pq_isready` 로 실제 연결 가능 여부를 확인한다.
- `depends_on: condition: service_healthy` : health check 통과 후에 의존 서비스를 시작한다.

# 요구사항 3: Docker 컨테이너 기반 테스트 아키텍처

## 아키텍처 변화

- Host-Container 분리 실행
    - 테스트만 호스트에서 실행되고, 앱과 DB는 각각 별도 컨테이너에서 실행된다
    - 테스트 ↔ 앱 사이에 실제 네트워크 통신이 발생한다.
    - 운영 배포 구조(앱 컨테이너 + DB 컨테이너)와 동이란 형태

## webEnvironment = NONE

- Spring이 웹 서버를 아예 띄우지 않음
- ApplicationContext만 로딩 → Bean 주입용
- 테스트 코드가 외부 URL(localhost:28080)로 직접 요청
- Docker 컨테이너의 앱이 그 요청을 받음
- 따라서 테스트 대상 앱이 JVM 안이 아닌, 외부 도커에서 뜨기 때문에 고정 포트로 보내도록 해야함

## Multi-stageDockerfile

### Single-stage의 문제

```docker
FROM eclipse-temurin:21-jdk
COPY . .
RUN ./gradlew bootJar
ENTRYPOINT ["java", "-jar", "build/libs/app.jar"]
```

- 이미지 안에 JDK 전체, Gradle, 소스코드, 빌드 캐시가 모두 포함된다.
- 실행에는 JRE와 jar 파일만 필요한데, 불필요한 파일까지 함께 이미지에 남게된다.

### Multi-stage가 해결하는 것

```docker
# Stage 1: 빌드 (도구가 필요)
FROM eclipse-temurin:21-jdk AS builder
COPY . .
RUN ./gradlew bootJar

# Stage 2: 실행 (결과물만 필요)
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- 의존성 레이어 캐싱 전략
    - Docker는 레이어 단위로 캐싱한다.
    - 소스 코드가 바뀌어도 `build.gradle` 이 안 바뀌었으면 의존성 다운로드 레이어는 캐시에서 재사용된다.
    - 소스만 바꾸면 빌드가 빠르다.

## Docker Network에서 서비스명이 hostname이 되는 이유

### Docker Compose의 자동 네트워크 생성

- docker-compose up 실행
    - “프로젝트명_default”라는 bridge 네트워크가 자동 생성된다
    - 모든 서비스가 이 네트워크에 연결된다
    - Docker 내장 DNS 서버가 서비스명 → 컨테이너 IP를 해석한다
        - Docker Compose가 각 서비스를 네트워크에 등록할 때 서비스명을 DNS 레코드로 등록한다.
