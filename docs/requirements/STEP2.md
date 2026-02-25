# 1. Cucumber BDD 적용

1단계: 의존성 추가 (build.gradle)

- cucumber-java, cucumber-spring, cucumber-junit-platform-engine 추가
- junit-platform-suite 추가 (테스트 실행용)

2단계: Cucumber + Spring Boot 통합 설정

- CucumberSpringConfiguration 클래스 생성
    - @CucumberContextConfiguration + @SpringBootTest(RANDOM_PORT)
- ScenarioContext 클래스 생성
    - @ScenarioScope Bean으로 시나리오 간 상태(Response, 엔티티 ID 매핑) 공유
- CucumberTest 러너 클래스 생성
    - JUnit Platform Suite API (@Suite + @IncludeEngines("cucumber"))
    - @SelectClasspathResource("features")로 Feature 파일 경로 지정
    - @ConfigurationParameter(GLUE_PROPERTY_NAME, "gift.cucumber")로 글루 경로 지정
- src/test/resources/cucumber.properties 생성 (publish 알림 비활성화)

3단계: 데이터 격리

- CucumberHooks 클래스 생성
- Cucumber @Before hook에서 매 시나리오 전 JdbcTemplate으로 cleanup 실행 (DELETE + IDENTITY 리셋)
- @Before hook에서 RestAssured 포트 설정 (@LocalServerPort 주입)
- 기존 @Sql + @BeforeEach 방식 대체

4단계: Feature 파일 작성 (src/test/resources/features/)

기존 3개 테스트 클래스를 한글 Gherkin(# language: ko)으로 전환:
- category.feature — 카테고리 생성/조회 (2 시나리오)
- product.feature — 상품 생성/조회, 존재하지 않는 카테고리 실패 (2 시나리오)
- gift.feature — 배경(Background)으로 공통 데이터 준비, 선물 보내기/재고 차감/실패 케이스 (4 시나리오)
- 한글 키워드 사용: 조건/만일/그러면/그리고

5단계: Step Definitions 구현 (io.cucumber.java.ko)

- Feature 파일의 한글 키워드에 맞춰 io.cucumber.java.ko 패키지의 @조건/@만일/@그러면 어노테이션 사용
- ScenarioContext를 확장하여 엔티티별 name→ID 매핑 + 시퀀스 카운터 추가
- CommonStepDefinitions: 응답 상태코드 검증
- CategoryStepDefinitions: 카테고리 등록(JdbcTemplate), 생성(API), 목록 조회/포함 검증
- ProductStepDefinitions: 상품 등록(JdbcTemplate), 생성(API), 목록 조회/포함 검증
- GiftStepDefinitions: 옵션/회원 등록(JdbcTemplate), 선물하기(API), 존재하지 않는 옵션 처리

6단계: 정리

- 기존 *AcceptanceTest 클래스는 유지 (Cucumber와 공존)
- README.md에 테스트 실행 방법 및 Cucumber BDD 시나리오 안내 추가

## 배운 것 정리
### CucumberSpringConfiguration(2단계)
- @CucumberContextConfiguration — Cucumber에게 "이 클래스가 Spring 설정의 진입점"이라고 알려줍니다. Cucumber는 이 어노테이션이 붙은 클래스를 찾아 Spring 컨텍스트를 부트스트랩합니다.
- @SpringBootTest(RANDOM_PORT) — 기존 AcceptanceTest에서 쓰던 것과 동일합니다. 실제 서버를 랜덤 포트로 띄워서 RestAssured로 HTTP 요청을 보낼 수 있게 합니다.

### ScenarioContext(2단계)
역할: 시나리오 내 Step 간 상태 공유 객체입니다.

왜 필요한가? Cucumber에서는 Given/When/Then이 각각 별도 메서드입니다:
When 선물을 보내면          → sendGift()    → response 저장
Then 상태코드 200을 받는다   → verifyStatus() → response 필요

When에서 받은 응답을 Then에서 검증하려면 공유 저장소가 필요합니다. ScenarioContext가 그 역할입니다.

@ScenarioScope가 핵심입니다:
- 각 시나리오마다 새로운 인스턴스가 생성됩니다
- 시나리오가 끝나면 자동으로 폐기됩니다
- 시나리오 A의 response가 시나리오 B에 영향을 주지 않습니다 → 데이터 격리

### CucumberTest(2단계)
./gradlew test 실행 시 Cucumber를 동작시키는 진입점입니다.

### CucumberHooks(3단계)
- 기존에는 AcceptanceTest의 @BeforeEach에서 했던 일을 Cucumber의 @Before hook으로 이동
- @Before은 매 시나리오 실행 전 호출.

### Feature 파일들(4단계)
- 기존 Java 인수 테스트의 시나리오를 자연어(Gherkin)로 옮긴 단계

### Step Definition 구현 (5단계)
- Gherkin 문장 하나하나에 실제 실행 코드를 연결한 단계
- CommonStepDefinitions, CategoryStepDefinitions 과 같은 것들을 코드와 평문과 연결


### 고려사항
- 재고 조회 API가 없는 현재 상황에서 발생할 수 있는 문제 

🟡 경고: gift.feature에서 "재고 차감" 결과를 상태코드로만 검증한다
파일: gift.feature:10-14

시나리오: 선물을 보내면 재고가 차감된다
만일 "보내는사람"이 "받는사람"에게 "Tall" 옵션으로 7개를 선물하면
그러면 응답 상태코드는 200이다
만일 "보내는사람"이 "받는사람"에게 "Tall" 옵션으로 5개를 선물하면
그러면 응답 상태코드는 500이다
문제: 시나리오 이름이 "재고가 차감된다"이지만, 실제로 재고가 차감되었는지(10→3) 직접 검증하지 않습니다. 후속 요청의 성공/실패로 간접 추론하는 방식입니다. CLAUDE.md의 테스트 규칙("결과 상태를 검증")에는 부합하지만, BDD 시나리오의 의도 전달력이 떨어집니다.

개선 방향: 재고 조회 API가 없는 현재 상황에서는 한계가 있지만, DB 직접 조회 Step을 추가하면 시나리오가 더 명확해집니다.

그러면 "Tall" 옵션의 재고는 3개이다


# 2. PostgreSQL + Docker Compose 통합
현재 상태

- DB: H2 in-memory (jdbc:h2:mem:testdb)
- Docker Compose: 없음
- PostgreSQL 의존성: 없음
- Spring Profile: 없음 (단일 application.properties)
- Gradle 테스트 태스크: test 하나만 존재
- cleanup.sql: H2 전용 문법 (ALTER TABLE ... ALTER COLUMN id RESTART WITH 1)

실행 순서

1단계: docker-compose.yml 작성
- PostgreSQL 서비스 정의 (이미지, 포트, 환경변수)
- healthcheck + pg_isready로 컨테이너 준비 상태 확인
- volume 설정 (선택)

2단계: PostgreSQL 의존성 추가 (build.gradle)
- runtimeOnly 'org.postgresql:postgresql' 추가
- H2는 기존 AcceptanceTest용으로 유지

3단계: Spring Profile 분리
- application-cucumber.properties 생성 (PostgreSQL 연결 정보)
- CucumberSpringConfiguration에 @ActiveProfiles("cucumber") 추가
- 기존 AcceptanceTest는 H2 그대로 유지 → H2/PostgreSQL 테스트 분리

4단계: PostgreSQL용 cleanup SQL 작성
- H2 문법 → PostgreSQL 문법 전환 필요
- TRUNCATE ... RESTART IDENTITY CASCADE 또는 DELETE + ALTER SEQUENCE RESTART
- CucumberHooks에서 PostgreSQL용 스크립트 사용

5단계: Gradle cucumberTest 태스크 생성
- doFirst: docker-compose up -d (PostgreSQL 시작 + healthcheck 대기)
- Cucumber 테스트만 실행 (기존 AcceptanceTest 제외)
- finalizedBy: docker-compose down (테스트 실패 시에도 DB 정리)

6단계: 검증 및 README 업데이트
- ./gradlew cucumberTest 실행 → PostgreSQL 자동 준비 + 테스트 통과 확인
- README.md에 실행 방법 업데이트

## 배운 것 정리

### Production Parity
왜 H2 대신 PostgreSQL을 사용하는가? — 프로덕션과 동일한 환경에서 테스트해야 "프로덕션에서만 발생하는 버그"를 사전에 잡을 수 있다는 원칙입니다.

실제로 이번 작업에서 체감한 H2와 PostgreSQL의 차이:
- cleanup SQL 문법이 다름: H2는 `ALTER TABLE ... ALTER COLUMN id RESTART WITH 1`, PostgreSQL은 `TRUNCATE ... RESTART IDENTITY CASCADE`
- H2에서는 통과하던 쿼리가 PostgreSQL에서 실패할 수 있음 (대소문자 처리, 타입 캐스팅 등)
- 이 차이 때문에 cleanup.sql(H2)과 cleanup-pg.sql(PostgreSQL)을 분리해야 했음

### docker-compose.yml (1단계)
- `services` — 실행할 컨테이너를 정의. 여기서는 PostgreSQL 하나만 정의
- `environment` — 컨테이너 시작 시 자동으로 DB, 유저, 패스워드를 생성
- `healthcheck` + `pg_isready` — 컨테이너가 떴다고 바로 쓸 수 있는 게 아님. PostgreSQL이 실제로 쿼리를 받을 준비가 됐는지 확인하는 것이 healthcheck의 역할
- `volumes`를 넣지 않은 이유 — 테스트용 DB는 매번 깨끗한 상태에서 시작하는 게 유리. 컨테이너 종료 시 데이터가 사라지는 것이 오히려 테스트 격리에 도움

### Spring Profile (3단계)
- `@ActiveProfiles("cucumber")` → Spring Boot가 `application-cucumber.properties`를 로드
- 동작 순서: `application.properties`(기본)를 먼저 로드 → `application-cucumber.properties`로 오버라이드
- 즉, datasource 설정이 H2 → PostgreSQL로 덮어씌워짐
- 기존 AcceptanceTest는 Profile이 없으므로 H2(기본)를 계속 사용 → 테스트 분리 달성

### TRUNCATE vs DELETE (4단계)
- `DELETE FROM` — 행 단위 삭제. 외래키 순서를 수동으로 지정해야 함 (wish → option → product → ...)
- `TRUNCATE` — 테이블 통째로 비움. DELETE보다 빠름
- `RESTART IDENTITY` — IDENTITY 시퀀스를 1로 리셋
- `CASCADE` — 외래키 의존 관계를 자동 처리. 삭제 순서를 신경 쓸 필요 없음
- H2에서는 TRUNCATE + RESTART IDENTITY CASCADE 문법이 다르므로, DB별로 cleanup 스크립트를 분리

### Gradle 커스텀 Test 태스크 (5단계)
- `tasks.register('cucumberTest', Test)` — 기본 `test` 태스크와 별개의 테스트 태스크 생성
- 커스텀 Test 태스크는 `testClassesDirs`와 `classpath`를 명시해야 함. 기본 `test` 태스크만 자동으로 소스 경로를 상속받음
- `include '**/CucumberTest.class'` — 이 태스크에서 실행할 테스트 클래스를 필터링
- `dependsOn 'dockerComposeUp'` — 테스트 실행 전 PostgreSQL 컨테이너를 먼저 시작
- `--wait` 플래그 — Docker Compose V2 기능. healthcheck가 healthy 상태가 될 때까지 대기. 이것이 없으면 PostgreSQL 시작 중에 테스트가 연결을 시도하여 Connection refused 발생 (경쟁 조건 방지)

### 네트워크 이해
- 테스트 코드(JVM)는 Host에서 실행됨
- PostgreSQL은 Docker 컨테이너 안에서 실행됨
- `ports: "5432:5432"` 매핑으로 Host의 localhost:5432 → 컨테이너의 5432로 연결
- 따라서 `application-cucumber.properties`에서 `jdbc:postgresql://localhost:5432/gift_test`로 접근 가능

### ddl-auto=create
- 매 애플리케이션 시작 시 기존 테이블을 DROP하고 엔티티 기반으로 재생성
- cucumberTest에서는 Spring Boot가 한 번만 뜨고 8개 시나리오가 모두 실행되므로, 시나리오 간 스키마 리셋은 일어나지 않음
- 시나리오 간 데이터 초기화는 TRUNCATE(CucumberHooks)가 담당
- 프로덕션에서는 create 대신 Flyway/Liquibase 같은 마이그레이션 도구를 사용해야 함


# Application 컨테이너화
현재 상태 (요구사항 2 완료 후)

테스트(Host JVM) ──→ 내장 Tomcat(Host JVM) ──→ PostgreSQL(Docker)                                                                                                                                              
HTTP(localhost:random)              JDBC(localhost:5432)
- 테스트와 애플리케이션이 같은 JVM에서 실행
- @SpringBootTest(RANDOM_PORT)가 내장 서버를 띄움

목표 상태 (요구사항 3)

테스트(Host JVM) ──HTTP──→ App(Docker, 28080:8080) ──JDBC──→ PostgreSQL(Docker)
│                       Docker Network (postgres:5432)
└──JDBC──→ PostgreSQL(Docker, localhost:5432)  ← cleanup용
- 애플리케이션이 Docker 컨테이너에서 실행
- 테스트는 Host에서 localhost:28080으로 HTTP 요청
- 테스트는 Host에서 localhost:5432로 JDBC 접속 (cleanup용)
- App 컨테이너는 Docker 네트워크 내부에서 postgres:5432로 DB 접속

실행 순서

1단계: Dockerfile 작성 (Multi-stage build)
- Builder stage: JDK 이미지 + Gradle Wrapper로 JAR 빌드
- Runtime stage: JRE 이미지 + JAR 복사 + 실행
- Multi-stage를 쓰는 이유: 빌드 도구(Gradle, JDK)를 최종 이미지에서 제거 → 이미지 크기 감소

2단계: .dockerignore 작성
- build/, .gradle/, .git/ 등 불필요한 파일 제외 → 빌드 컨텍스트 축소

3단계: docker-compose.yml 업데이트
- app 서비스 추가
- depends_on: postgres (condition: service_healthy) — PostgreSQL 준비 후 앱 시작
- ports: 28080:8080 — Host에서 접근 가능
- environment — SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/gift_test (Docker 내부 네트워크에서는 서비스명이 hostname)
- app의 healthcheck 추가

4단계: CucumberSpringConfiguration 변경
- webEnvironment = RANDOM_PORT → webEnvironment = NONE
- 내장 서버 제거. 앱은 Docker에서 실행되므로 테스트 JVM에서 서버를 띄울 필요 없음
- Spring 컨텍스트는 로드되므로 JdbcTemplate(cleanup용)은 사용 가능

5단계: application-cucumber.properties 수정
- ddl-auto=create → ddl-auto=none (스키마 생성은 Docker 앱이 담당)
- datasource는 localhost:5432 유지 (테스트의 JDBC cleanup용)

6단계: CucumberHooks 수정
- @LocalServerPort 제거 (내장 서버 없음)
- RestAssured.baseURI = "http://localhost", RestAssured.port = 28080으로 변경

7단계: Gradle 태스크 정리
- dockerBuild — Docker 이미지 빌드
- dockerUp — docker compose up -d --wait (기존 dockerComposeUp 대체)
- dockerDown — docker compose down (기존 dockerComposeDown 대체)
- cucumberTest — dependsOn 'dockerUp', dockerUp은 dependsOn 'dockerBuild'

8단계: 검증 및 README 업데이트

./gradlew dockerBuild      # Docker 이미지 빌드
./gradlew dockerUp          # PostgreSQL + App 시작
curl http://localhost:28080  # 앱 응답 확인
./gradlew cucumberTest       # 테스트 실행
./gradlew dockerDown         # 정리

## 배운 것 정리

### Multi-stage build (1단계)
Dockerfile을 여러 단계로 나누는 빌드 방식:
- Builder stage (`eclipse-temurin:25-jdk`): JDK + Gradle로 JAR을 빌드. 이 레이어는 최종 이미지에 포함되지 않음
- Runtime stage (`eclipse-temurin:25-jre`): JRE + JAR만 포함. 빌드 도구, 소스 코드, 캐시가 모두 제거됨
- `COPY --from=builder`로 이전 stage의 결과물만 가져옴
- JDK(~400MB) 대신 JRE(~200MB)만 사용하므로 이미지 크기가 절반으로 줄어듦

Docker 레이어 캐싱도 활용:
- Gradle 설정 파일(build.gradle 등)을 먼저 복사 → 의존성 다운로드 → 소스 코드 복사 → 빌드
- 소스 코드만 바뀌면 의존성 다운로드 레이어는 캐시를 재사용하여 빌드가 빨라짐

### .dockerignore (2단계)
- `.gitignore`와 동일한 개념. Docker 빌드 시 컨텍스트(Docker 데몬으로 전송되는 파일)에서 제외할 항목을 지정
- `build/`, `.gradle/`, `.git/`, `src/test/` 등 빌드에 불필요한 파일을 제외
- 컨텍스트 크기가 줄어들어 빌드가 빨라짐

### Docker 네트워크와 서비스명 (3단계)
Docker Compose는 자동으로 네트워크를 생성하고, 서비스명이 그 네트워크 안에서 hostname이 됨:
- App 컨테이너에서 PostgreSQL 접근: `postgres:5432` (서비스명이 hostname)
- Host에서 PostgreSQL 접근: `localhost:5432` (포트 매핑)
- Host에서 App 접근: `localhost:28080` (포트 매핑 28080:8080)

`depends_on: condition: service_healthy`는 단순히 컨테이너 시작이 아니라, healthcheck가 통과할 때까지 기다림. 없으면 PostgreSQL이 아직 준비되지 않은 상태에서 앱이 연결을 시도하여 실패할 수 있음.

### webEnvironment = NONE (4단계)
- `RANDOM_PORT`: Spring Boot가 내장 Tomcat을 랜덤 포트로 시작. 테스트와 앱이 같은 JVM에서 실행
- `NONE`: 웹 서버를 시작하지 않음. Spring 컨텍스트(Bean)만 로드
- 앱이 Docker 컨테이너에서 실행되므로 테스트 JVM에서 서버를 띄울 필요 없음
- 하지만 Spring 컨텍스트가 로드되므로 JdbcTemplate은 사용 가능 → cleanup용 JDBC 접속에 활용

### 환경변수로 Spring Boot 설정 오버라이드 (트러블슈팅)
Spring Boot는 환경변수를 properties로 자동 바인딩:
- `SPRING_DATASOURCE_URL` → `spring.datasource.url`
- `SPRING_JPA_HIBERNATE_DDL_AUTO` → `spring.jpa.hibernate.ddl-auto`

주의: `application.properties`에 하드코딩된 `spring.datasource.driverClassName=org.h2.Driver`가 있으면, URL만 환경변수로 PostgreSQL로 바꿔도 드라이버가 H2로 남아서 Dialect 감지에 실패함. `SPRING_DATASOURCE_DRIVER_CLASS_NAME`도 환경변수로 오버라이드해야 함.

### ddl-auto 역할 분리 (5단계)
- Docker 앱 (docker-compose.yml): `DDL_AUTO: create` → 스키마 생성 담당
- 테스트 JVM (application-cucumber.properties): `ddl-auto=none` → 스키마 건드리지 않음
- 둘 다 `create`면 Docker 앱이 만든 테이블을 테스트 JVM이 DROP + 재생성하여 충돌 가능

### healthcheck에 curl이 필요 (트러블슈팅)
- `eclipse-temurin:25-jre` 이미지에는 `curl`이 기본 설치되어 있지 않음
- healthcheck에서 `curl -f http://localhost:8080/api/categories`를 사용하므로, Dockerfile runtime stage에 `apt-get install -y curl` 추가 필요
- `rm -rf /var/lib/apt/lists/*`로 패키지 캐시 정리하여 이미지 크기 최소화

### Gradle 커스텀 Exec 태스크
- `tasks.register('dockerBuild', Exec)` — 외부 명령어(`docker compose build`)를 Gradle 태스크로 실행
- `dependsOn` 체인: `cucumberTest` → `dockerUp` → `dockerBuild`로 자동 빌드 + 시작
- `finalizedBy 'dockerDown'` — 테스트 성공/실패 무관하게 컨테이너 정리