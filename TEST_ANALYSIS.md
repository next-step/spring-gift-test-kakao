# 인수 테스트 분석 보고서

## 분석 기준

TEST_STRATEGY.md에 따라 **API 엔드포인트 단위로 행위를 식별**하고, **HTTP 계약 / 관찰 가능한 상태 결과 / 사이드 이펙트**를 검증 대상으로 삼았다. 각 시나리오는 **리스크 기반으로 자동화 여부를 판단**하고, Gherkin으로 기술한다.

---

## 1. 현재 식별된 API 엔드포인트

| # | Method | URI | Controller | 비고 |
|---|--------|-----|------------|------|
| 1 | POST | `/api/categories` | CategoryRestController | `@RequestBody` 없음 (form 바인딩) |
| 2 | GET | `/api/categories` | CategoryRestController | |
| 3 | POST | `/api/products` | ProductRestController | `@RequestBody` 없음 (form 바인딩) |
| 4 | GET | `/api/products` | ProductRestController | |
| 5 | POST | `/api/gifts` | GiftRestController | `@RequestBody` + `@RequestHeader("Member-Id")` |

## 2. API 엔드포인트가 없는 서비스 (주의)

| Service | 메서드 | Controller | 문제점 |
|---------|--------|------------|--------|
| **OptionService** | create, retrieve | 없음 | Option을 API로 생성할 수 없음. 선물 테스트의 사전 데이터를 API로 준비 불가 |
| **WishService** | create | 없음 | Wish 기능을 API로 검증할 수 없음 |

이 두 서비스는 API 엔드포인트가 없으므로 인수 테스트의 직접적인 검증 대상이 아니다. 다만 **Option**은 선물 전달 테스트의 선행 데이터이므로, **Step Definitions 내에서 Repository 레벨로 준비**해야 한다.

---

## 3. 리스크 평가

### User Story

> 카카오 선물하기 사용자로서, 친구에게 선물을 보내고 싶다. 특별한 날을 축하할 수 있도록.

### 시나리오별 리스크

| 시나리오 | Impact | Likelihood | Detection Cost | Risk |
|----------|--------|------------|----------------|------|
| 재고 충분 시 선물 발송 성공 | High | High | High | **High** |
| 재고 부족 시 선물 발송 실패 | High | High | High | **High** |
| 상품 생성 성공 | Medium | Medium | Low | **Medium** |
| 상품 생성 후 조회 반영 | Medium | Medium | Low | **Medium** |
| 존재하지 않는 옵션으로 선물 발송 | Medium | Low | Medium | **Medium** |
| 존재하지 않는 카테고리로 상품 생성 | Medium | Low | Medium | **Medium** |
| 카테고리 생성 성공 | Low | Low | Low | **Low** |
| 카테고리 생성 후 조회 반영 | Low | Low | Low | **Low** |

---

## 4. 행위별 Gherkin 시나리오

### 행위 1: 카테고리 생성 (`POST /api/categories`)

```gherkin
Feature: 카테고리 관리

  Scenario: 유효한 이름으로 카테고리를 생성한다
    When "음료" 카테고리를 생성한다
    Then 카테고리 생성이 성공한다
    And 응답에 카테고리 이름 "음료"가 포함되어 있다

  Scenario: 생성한 카테고리가 조회 목록에 반영된다
    Given "음료" 카테고리가 존재한다
    When 전체 카테고리를 조회한다
    Then 조회 결과에 "음료" 카테고리가 포함되어 있다
```

### 행위 2: 상품 생성 (`POST /api/products`)

```gherkin
Feature: 상품 관리

  Scenario: 유효한 데이터로 상품을 생성한다
    Given "음료" 카테고리가 존재한다
    When "아메리카노" 상품을 가격 4500원으로 생성한다
    Then 상품 생성이 성공한다
    And 응답에 상품 이름 "아메리카노"가 포함되어 있다

  Scenario: 생성한 상품이 조회 목록에 반영된다
    Given "음료" 카테고리가 존재한다
    And "아메리카노" 상품이 존재한다
    When 전체 상품을 조회한다
    Then 조회 결과에 "아메리카노" 상품이 포함되어 있다

  Scenario: 존재하지 않는 카테고리로 상품을 생성하면 실패한다
    When 존재하지 않는 카테고리로 상품을 생성한다
    Then 상품 생성이 실패한다
```

### 행위 3: 선물 전달 (`POST /api/gifts`)

```gherkin
Feature: 선물 전달

  Scenario: 재고가 충분하면 선물 발송에 성공한다
    Given "ICE" 옵션의 재고가 10개 있다
    And 회원이 존재한다
    When 회원이 "ICE" 옵션 1개를 선물한다
    Then 선물 발송이 성공한다
    And 해당 옵션의 재고가 9개이다

  Scenario: 재고가 부족하면 선물 발송에 실패한다
    Given "ICE" 옵션의 재고가 1개 있다
    And 회원이 존재한다
    When 회원이 "ICE" 옵션 2개를 선물한다
    Then 재고 부족으로 실패한다

  Scenario: 존재하지 않는 옵션으로 선물하면 실패한다
    Given 회원이 존재한다
    When 존재하지 않는 옵션으로 선물한다
    Then 선물 발송이 실패한다
```

---

## 5. 테스트 데이터 준비

API 엔드포인트가 있는 도메인(Category, Product)은 **Step Definitions에서 API를 통해** 생성한다. API가 없는 도메인(Option, Member)은 **Repository를 통해 직접** 생성한다. 각 시나리오 시작 전 `@Before` 훅에서 **전체 테이블을 truncate**하여 데이터를 격리한다.

```java
// Step Definition 내부 예시
@Given("{string} 카테고리가 존재한다")
public void 카테고리_존재(String name) {
    // API 호출로 생성
}

@Given("{string} 옵션의 재고가 {int}개 있다")
public void 옵션_재고_설정(String optionName, int quantity) {
    // Repository로 직접 생성 (API 엔드포인트 없음)
}
```

---

## 6. 테스트 구조

```
src/test/
├── java/gift/
│   ├── cucumber/
│   │   ├── CucumberTest.java              # Cucumber 실행 진입점
│   │   ├── ScenarioContext.java            # Step 간 상태 공유
│   │   ├── DatabaseCleanup.java           # @Before 훅 — 시나리오별 DB 초기화
│   │   └── steps/
│   │       ├── CategoryStepDefinitions.java
│   │       ├── ProductStepDefinitions.java
│   │       └── GiftStepDefinitions.java
│   └── support/
│       ├── FakeGiftDelivery.java           # Test Double
│       ├── CategoryFixture.java
│       ├── ProductFixture.java
│       ├── OptionFixture.java
│       └── MemberFixture.java
└── resources/
    └── features/
        ├── category.feature
        ├── product.feature
        └── gift.feature

# 프로젝트 루트
├── Dockerfile                              # Multi-stage build
├── docker-compose.yml                      # PostgreSQL + Application
```

---

## 7. 종합 정리

| 항목 | 수치 |
|------|------|
| 검증 대상 행위 수 | **5개** (TEST_STRATEGY.md 최소 기준 충족) |
| 총 Gherkin 시나리오 수 | **8개** (자동화 대상) |
| Feature 파일 수 | **3개** |
| Step Definition 클래스 수 | **3개** |

### 핵심 참고사항

- `OptionService`, `WishService`는 컨트롤러가 없어 API 레벨 검증 불가 — Step Definitions에서 Repository로 직접 준비
- `CategoryRestController`, `ProductRestController`의 `create` 메서드에 `@RequestBody`가 없으므로, 테스트 시 JSON이 아닌 form parameter 방식으로 요청해야 함
- 테스트 DB는 Docker Compose로 기동하는 PostgreSQL을 사용하며, Spring 프로파일로 테스트/개발 환경을 분리한다
- 각 시나리오 전 `@Before` 훅에서 전체 테이블을 truncate하여 데이터 격리를 보장한다