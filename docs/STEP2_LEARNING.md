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
