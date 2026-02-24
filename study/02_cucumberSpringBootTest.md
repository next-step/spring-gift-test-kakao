# Cucumber Spring Boot 통합 테스트 구조

## 전체 구조 요약

1. CucumberSpringConfiguration: Cucumber가 Spring Context를 로드할 수 있도록 해 준다.
2. ScenarioContext: When 단계에서 얻은 결과를 Then 단계로 전달한다.
3. GiftStepDefinitions: .feature 파일의 문장을 실제 Java 코드로 변환해서 실행한다.

## 파일별 상세 분석

### CucumberSpringConfiguration

- Cucumber 테스트가 실행될 때 Spring Boot 서버 띄우고 Bean 사용할 수 있게 해주는 설정 파일

- @CucumberContextConfiguration
  - 시나리오 시작 전 Spring 설정을 읽어오도록 알려준다.
- @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  - 실제 Spring Boot 애플리케이션을 테스트 모드로 구동한다.
  - 실제 내장 톰캣 서버를 랜덤 포트로 띄운다.

### ScenarioContext.java

- Cucumber의 각 Step(Given, When, Then)은 메서드가 서로 분리되어 있다. State 저장을 위한 객체이다.

- @ScenarioScope
  - 이 빈의 생명주기를 하나의 시나리오로 제한한다.
    - 즉, 시나리오 A가 끝나고 시나리오 B가 시작되면, 이 객체는 새로 생성된다.
    - 테스트끼리 데이터가 섞이는 문제를 방지한다.

### GiftStepDefinitions.java

- Gherkin 문장을 실행 가능한 코드로 매핑한다.
    - @Before - 일반적으로 모든 테이블 초기화 용도
    - @Given
    - @When
    - @Then

- 정규 표현식
    - ^...$ : .feature 파일의 문장에서 변수 추출
    - \\d+ : 숫자
    - [^\"*] : 따옴표 내 문자열
