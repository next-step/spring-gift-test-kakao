# 선물하기 서비스 - Cucumber BDD 테스트 가이드

기존 RestAssured + JUnit5 기반 인수 테스트를 **Cucumber BDD** 형식으로 전환하여, 비개발자도 이해할 수 있는 테스트 시나리오를 작성한다.

---

## 핵심 원칙

1. **한글 Gherkin 시나리오:** `조건/만일/그러면` 한글 키워드로 비즈니스 행동을 묘사한다.
2. **상태 변화 검증:** 실패 시나리오를 통해 재고 부족 등 상태 변화를 증명한다.
3. **시나리오 간 데이터 격리:** Cucumber `@Before` 훅에서 DB를 TRUNCATE하여 테스트 간 간섭을 방지한다.
4. **Step 재사용:** Given/When/Then 스텝을 재사용 가능하게 작성한다.

---

## 기술 스택

| 구성 요소 | 기술 |
| --- | --- |
| BDD 프레임워크 | Cucumber 7.x (`cucumber-java`, `cucumber-spring`) |
| 한글 스텝 | `io.cucumber.java.ko` 패키지 |
| HTTP 테스트 | RestAssured |
| Spring 통합 | `@CucumberContextConfiguration` + `@SpringBootTest` |
| DB | H2 In-Memory (기존 유지) |
| 테스트 러너 | JUnit Platform Suite API |

---

## 의존성 (build.gradle 추가분)

```groovy
testImplementation 'io.cucumber:cucumber-java:7.22.0'
testImplementation 'io.cucumber:cucumber-spring:7.22.0'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.22.0'
testImplementation 'org.junit.platform:junit-platform-suite'
```

---

## 디렉토리 구조

```
src/test/
├── java/gift/
│   ├── cucumber/
│   │   ├── CucumberSpringConfiguration.java   # Spring 통합 + DB 초기화 훅
│   │   ├── CucumberTest.java                  # JUnit Platform Suite 러너
│   │   ├── TestContext.java                    # @ScenarioScope 상태 공유
│   │   └── steps/                             # Step Definitions
│   │       ├── CommonSteps.java
│   │       ├── CategorySteps.java
│   │       ├── ProductSteps.java
│   │       └── GiftSteps.java
│   ├── CategoryAcceptanceTest.java            # 기존 테스트 (유지)
│   ├── ProductAcceptanceTest.java             # 기존 테스트 (유지)
│   └── GiftAcceptanceTest.java                # 기존 테스트 (유지)
├── resources/
│   ├── features/                              # Gherkin 시나리오 파일
│   │   ├── category.feature
│   │   ├── product.feature
│   │   └── gift.feature
│   └── data/                                  # 기존 SQL 시드 파일 (유지)
```

---

## 기존 테스트와의 공존

- 기존 `*AcceptanceTest` 클래스는 그대로 유지한다.
- `./gradlew test` 실행 시 기존 JUnit5 테스트와 Cucumber 시나리오가 모두 통과해야 한다.

---

## 작업 순서

| 순서 | 작업 |
| --- | --- |
| 1 | `build.gradle`에 Cucumber 의존성 추가 |
| 2 | `CucumberSpringConfiguration`, `CucumberTest` 러너 작성 |
| 3 | `TestContext`, `CommonSteps`, DB 초기화 훅 구현 |
| 4 | `category.feature` + `CategorySteps` |
| 5 | `product.feature` + `ProductSteps` |
| 6 | `gift.feature` + `GiftSteps` |
| 7 | `./gradlew test`로 전체 검증 |

---

## 참고

- [Cucumber 공식 문서](https://cucumber.io/docs/cucumber/)
- [Cucumber-Spring 통합](https://cucumber.io/docs/cucumber/state/#spring)
