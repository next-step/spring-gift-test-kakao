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
