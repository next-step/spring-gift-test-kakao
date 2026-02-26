## io.cucumber:cucumber-spring
cucumber와 spring boot 를 연결해주는 통합 모듈

이게 하는 일:
- Cucumber가 Steps 인스턴스를 생성할 때 Spring ApplicationContext에서 빈을
  찾아 생성자에 주입해줌
- @CucumberContextConfiguration이 붙은 클래스를 찾아 Spring Boot 테스트
  컨텍스트를 시작함

즉, Cucumber의 테스트 라이프사이클과 Spring의 DI 컨테이너를 이어주는 브릿지
역할입니다.

## @CucumberContextConfiguration

위는 spring context 설정을 가져오라는 어노테이션
위로 spring context 를 가져와서 DI 등을 수행합니다.
또한 이는 아래 scan 범위에 하나의 config class 만을 가져야 합니다.

## @ScenarioScope

시나리오간 상태 격리를 위한 설정
각각의 시나리오마다 scenarioContext 를 생성하고 이에 response 를 저장합니다.

Response를 저장하는 이유는 Steps 클래스가 분리되어 있기 때문입니다.

서로 다른 클래스이므로 Response를 직접 전달할 방법이 없습니다. 그래서 ScenarioContext라는 공유 객체에 저장하고, 양쪽에서 꺼내 쓰는 것입니다.

CategorySteps → context.setResponse(response)
↓
ScenarioContext (공유)
↓
CommonSteps   → context.getResponse().then().statusCode(200)

## io.cucumber.java.ko

io.cucumber.java.ko는 한글 키워드용 어노테이션 패키지
given, when, then 등을 매핑합니다.

## featurefile

테스트 시나리오를 작성하는 파일

## Step Definitions에서 파라미터를 어떻게 추출하는가?

```java
@When("회원 {string}이 회원 {string}에게 옵션 {string} 수량 {int} 메시지 {string}로 선물을
  전송하면")
  public void 선물_전송(String senderName, String receiverName, String optionName, int quantity,
  String message) {
      ...
  }
```
위처럼 추출하여 인자로 넘깁니다.

## 시나리오 간 Response 객체를 어떻게 공유하는가?

scenario context 에서 공유합니다.

## @Before hook은 언제 실행되는가?

매 시나리오 실행 직전에 실행됩니다.
