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

## 2-3-4. doFirst 키워드 반영
- **Prompt**: 요구사항 키워드 점검 결과 `doFirst`가 누락됨. `dependsOn dockerUp` 방식에서 `doFirst` 인라인 방식으로 변경.
- **Action**:
  - `build.gradle`: 별도 `dockerUp` Exec 태스크 제거. cucumberTest 내부에 `doFirst { exec { commandLine 'docker-compose', 'up', '-d', '--wait' } }`로 인라인 실행하도록 변경. `finalizedBy dockerDown`은 유지.
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL. doFirst로 Docker 기동 → 6개 시나리오 통과 → finalizedBy로 Docker 정리 확인.

## 2-4. 애플리케이션 컨테이너화 (Step 2-3)
- **Prompt**: Spring Boot 앱 실행할 Dockerfile 작성 (Multi-stage build). docker-compose.yml에 app 서비스 추가해서 DB와 같이 돌릴 수 있도록 수정. app 컨테이너는 DB 컨테이너에 의존하는 형태.
- **Action**:
  - `Dockerfile` 생성: Multi-stage build (`FROM eclipse-temurin:21-jdk AS builder` → `COPY --from=builder` → `FROM eclipse-temurin:21-jre-alpine` 경량 런타임). Windows CRLF 대응 `sed -i 's/\r$//' gradlew` 포함.
  - `docker-compose.yml`: app 서비스 추가. `depends_on: postgres: condition: service_healthy`로 시작 순서 보장. `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift` (Docker network에서 service name이 hostname). Port mapping `28080:8080`. app healthcheck(`wget --spider`로 `GET /api/categories` 활용).
  - `CucumberSpringConfiguration.java`: `webEnvironment = RANDOM_PORT` → `webEnvironment = NONE`으로 변경 (embedded 서버 제거, Docker 컨테이너의 앱 사용).
  - `CommonStepDefinitions.java`: `@LocalServerPort` 제거, `RestAssured.baseURI = "http://localhost"`, `RestAssured.port = 28080`으로 Docker 컨테이너 직접 호출.
  - `build.gradle`: doFirst의 docker-compose 명령에 `--build` 플래그 추가.
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL. Docker 이미지 빌드 → postgres + app 컨테이너 기동 → app Healthy 확인 → Cucumber 6개 시나리오 통과 → 전체 컨테이너 정리. `./gradlew test` BUILD SUCCESSFUL (RestAssured 6개, H2) — 기존 테스트 영향 없음 확인.

## 2-4-2. PostgreSQL 외부 포트 변경 (가독성 개선)
- **Prompt**: postgres도 외부 포트에서 내부 포트로 가는 거니까 안 헷갈리게 포트 번호 바꾸자.
- **Action**:
  - `docker-compose.yml`: postgres 포트 매핑 `5432:5432` → `25432:5432`로 변경. app의 `SPRING_DATASOURCE_URL`은 Docker 내부 네트워크(`postgres:5432`)이므로 변경 없음.
  - `application-cucumber.properties`: datasource URL `localhost:5432` → `localhost:25432`로 변경.
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL (Cucumber 6개, PostgreSQL `25432:5432`). `./gradlew test` BUILD SUCCESSFUL (RestAssured 6개, H2). 호스트 포트(`25432`, `28080`)와 컨테이너 내부 포트(`5432`, `8080`)가 명확히 구분됨.

## 2-4-3. study 문서 오류 수정
- **Prompt**: study/03_cucumberDockerPostgreSQLTest.md의 build.gradle 부분 서술이 실제 코드와 안 맞는 부분 수정.
- **Action**:
  - build.gradle 섹션: `useJUnitPlatform()`에 `includeEngines 'cucumber'` 누락 수정, `--build` 플래그 추가, `testClassesDirs`/`classpath` 설정 추가, `shouldRunAfter test` 추가
  - docker-compose 섹션: `SPRING_DATASOUCE_URL` → `SPRING_DATASOURCE_URL` 오타 수정
- **Outcome**: study 문서가 실제 구현 코드와 일치하도록 갱신 완료.

## 2-5. README.md 실행 방법 추가
- **Prompt**: README.md에 실행 방법 추가. 전제 조건, 명령어 구분(test/cucumberTest), 테스트 아키텍처(28080/25432 포트) 포함.
- **Action**:
  - `README.md`: 전제 조건(Java 21, Docker), 빌드/테스트 명령어, 테스트 아키텍처 다이어그램(Host → App 컨테이너 :28080, Host → PostgreSQL 컨테이너 :25432) 작성.
- **Outcome**: README.md 작성 완료.

## 2-6. OptionRepository 의존 제거 — API-only 행동 검증으로 전환
- **Prompt**: 피드백 반영. GiftStepDefinitions에서 OptionRepository로 재고를 직접 확인하는 방식을 제거하고, API 응답(성공/실패)만으로 재고 변화를 증명하는 행동 기반 시나리오로 재구성.
- **Action**:
  - `gift.feature`: 5개 시나리오로 재구성. `옵션 N의 재고는 N개이다` Then 스텝 제거. 대신 연속 선물 시도의 성공/실패 조합으로 재고 차감을 간접 증명 (예: 10개 중 3개 선물 → 200, 7개 더 → 200, 1개 더 → 500으로 정확한 차감량 증명. 실패 시 재고 미차감은 5개 시도 → 500 후 2개 시도 → 200으로 증명).
  - `GiftStepDefinitions.java`: `OptionRepository` import/필드/`옵션의_재고를_확인한다` 스텝 메서드 완전 제거. Option, assertThat import도 제거.
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL (Cucumber 7 시나리오). `./gradlew test` BUILD SUCCESSFUL (RestAssured 6개). 프로덕션 코드(Repository) 의존 없이 순수 API 블랙박스 테스트 달성.
- **교훈**: 인수 테스트는 시스템의 외부 인터페이스(API)만으로 검증해야 한다. 내부 구현(Repository)에 의존하면 화이트박스 테스트가 되어 인수 테스트 목적에 어긋남.
