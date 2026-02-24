# STEP 2: 인수 테스트 체계 고도화 - AI 사용 기록

## 2-1. Cucumber BDD 의존성 추가 및 Feature 파일 작성
- **Prompt**: Cucumber 의존성을 build.gradle에 추가하고, GiftApiTest의 4개 시나리오를 Gherkin 한글 문법으로 gift.feature 파일로 변환
- **Action**:
  - `build.gradle`: cucumber-java, cucumber-spring, cucumber-junit-platform-engine, junit-platform-suite 의존성 추가. cucumberTest Gradle 태스크 등록 (includeTags 'cucumber')
  - `src/test/resources/features/gift.feature`: 배경(공통 데이터) + 4개 시나리오(성공, 경계값, 재고부족, 재고0) 작성
- **Outcome**: 의존성 정상 해석 확인 완료. Feature 파일 작성 완료. Step Definition은 아직 미구현.

## 2-1-2. Step Definitions 및 Cucumber 설정 구현
- **Prompt**: gift.feature를 실행할 수 있도록 GiftStepDefinitions, CucumberSpringConfiguration 작성. ScenarioContext 패턴 적용. ./gradlew test로 검증.
- **Action**:
  - `src/test/java/gift/cucumber/ScenarioContext.java`: @Component + @ScenarioScope로 시나리오 간 상태(응답 코드) 공유
  - `src/test/java/gift/cucumber/CucumberSpringConfiguration.java`: @CucumberContextConfiguration + @SpringBootTest(RANDOM_PORT)
  - `src/test/java/gift/cucumber/GiftStepDefinitions.java`: 7개 Step 메서드 구현 (JdbcTemplate으로 데이터 셋업, RestAssured로 API 호출, OptionRepository로 DB 검증). @Before 훅에서 테이블 초기화.
  - `src/test/resources/junit-platform.properties`: Cucumber 엔진 설정 (features, glue, plugin)
  - `build.gradle`: cucumberTest 태스크의 includeTags → includeEngines 'cucumber'로 수정
- **Outcome**: `./gradlew test` BUILD SUCCESSFUL. Cucumber 4개 시나리오 + 기존 RestAssured 6개 테스트 모두 통과.

## 2-2. 기존 API 범위 내 BDD 시나리오 확장
- **Prompt**: Step 1의 나머지 케이스를 BDD feature로 추가. 단, 인수 테스트 목적에 맞게 기존 API만 검증 — 테스트를 위해 새 컨트롤러를 만들지 않음.
- **Action**:
  - `features/product.feature` 추가: 상품 등록 성공 / 존재하지 않는 카테고리 실패 (2 시나리오)
  - `ProductStepDefinitions.java` 추가: 상품 등록 When + 상품 수/카테고리 검증 Then
  - `CommonStepDefinitions.java` 분리: 공통 스텝 (@Before DB 초기화, 회원/카테고리/상품 Given, 응답 코드 Then)
  - `GiftStepDefinitions.java` 리팩토링: 선물 전용 스텝만 남김
  - Option/Wish는 REST 엔드포인트가 없으므로 인수 테스트 범위에서 제외
  - 처음에 생성했던 OptionRestController, WishRestController, option.feature, wish.feature, OptionStepDefinitions, WishStepDefinitions 모두 제거
- **Outcome**: `./gradlew clean test` BUILD SUCCESSFUL. Cucumber 6 시나리오 (gift 4 + product 2) + 기존 RestAssured 6개 = 총 12개 통과.
- **교훈**: 인수 테스트는 기존 시스템의 외부 동작을 검증하는 것이 목적. 테스트를 위해 프로덕션 코드(API)를 추가하는 것은 부적절.

## 2-3. PostgreSQL 도입 (Docker + 프로파일 분리)
- **Prompt**: Docker Compose로 PostgreSQL 15 서비스 정의 (healthcheck 포함). application-cucumber.properties로 프로파일 분리하여 Cucumber 테스트가 Docker PostgreSQL을 바라보도록 설정.
- **Action**:
  - `docker-compose.yml` 생성: PostgreSQL 15, 포트 5432, DB/유저/패스워드 `gift`, `pg_isready` healthcheck (interval 5s, timeout 3s, retries 5)
  - `src/main/resources/application-cucumber.properties` 생성: datasource URL(`jdbc:postgresql://localhost:5432/gift`), PostgreSQLDialect, ddl-auto=create-drop
  - `build.gradle`: `runtimeOnly 'org.postgresql:postgresql'` 의존성 추가
- **Outcome**: `docker-compose up -d` → 컨테이너 healthy 상태 확인. `./gradlew clean build -x test` BUILD SUCCESSFUL.

## 2-3-2. cucumberTest 태스크 Docker 라이프사이클 통합
- **Prompt**: cucumberTest 태스크가 docker-compose up → Cucumber 테스트 (cucumber 프로파일) → docker-compose down 순서로 자동 실행되도록 build.gradle 수정.
- **Action**:
  - `build.gradle`: `dockerUp`(Exec, `docker-compose up -d --wait`), `dockerDown`(Exec, `docker-compose down`) 태스크 추가. cucumberTest에 `dependsOn dockerUp`, `finalizedBy dockerDown`, `systemProperty 'spring.profiles.active', 'cucumber'` 설정
  - `CucumberSpringConfiguration.java`: `@ActiveProfiles("cucumber")` 추가
  - `CommonStepDefinitions.java`: H2 전용 `SET REFERENTIAL_INTEGRITY FALSE/TRUE` → PostgreSQL 호환 `TRUNCATE ... CASCADE`로 변경
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL. Docker 자동 기동 → 6개 Cucumber 시나리오 통과 → Docker 자동 정리 확인.

## 2-3-3. test/cucumberTest 태스크 엔진 분리
- **Prompt**: 기존 RestAssured 테스트가 `./gradlew test`로 여전히 정상 동작하는지 확인.
- **Action**:
  - `build.gradle`: `test` 태스크에 `excludeEngines 'cucumber'` 추가. Cucumber 테스트가 Docker 없이 실행되어 PostgreSQL 연결 실패하는 문제 해결.
- **Outcome**: `./gradlew test` BUILD SUCCESSFUL (RestAssured 6개, H2). `./gradlew cucumberTest` BUILD SUCCESSFUL (Cucumber 6개, PostgreSQL). 두 태스크 완전 분리 확인.
