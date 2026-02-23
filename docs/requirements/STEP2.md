# Cucumber BDD 적용

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