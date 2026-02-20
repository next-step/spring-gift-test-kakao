# 인수 테스트 분석 보고서

## 분석 기준

TEST_STRATEGY.md에 따라 **API 엔드포인트 단위로 행위를 식별**하고, **HTTP 계약 / 관찰 가능한 상태 결과 / 사이드 이펙트**를 검증 대상으로 삼았다.

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

이 두 서비스는 API 엔드포인트가 없으므로 인수 테스트의 직접적인 검증 대상이 아니다. 다만 **Option**은 선물 전달 테스트의 선행 데이터이므로, **Fixture를 통해 Repository 레벨에서 준비**해야 한다.

---

## 3. 행위별 테스트 케이스 설계

### 행위 1: 카테고리 생성 (`POST /api/categories`)

| TC | 시나리오 | 검증 항목 |
|----|---------|-----------|
| 1-1 | 유효한 이름으로 카테고리 생성 | HTTP 200, 응답 body에 id와 name 포함 |
| 1-2 | 생성한 카테고리가 조회 목록에 반영되는지 확인 | POST 후 GET으로 관찰 가능한 상태 변화 검증 |

### 행위 2: 카테고리 조회 (`GET /api/categories`)

| TC | 시나리오 | 검증 항목 |
|----|---------|-----------|
| 2-1 | 카테고리가 없을 때 빈 목록 반환 | HTTP 200, 빈 배열 `[]` |

### 행위 3: 상품 생성 (`POST /api/products`)

| TC | 시나리오 | 검증 항목 |
|----|---------|-----------|
| 3-1 | 유효한 데이터로 상품 생성 (카테고리 선행 생성 필요) | HTTP 200, 응답에 id, name, price, imageUrl 포함 |
| 3-2 | 생성한 상품이 조회 목록에 반영되는지 확인 | POST 후 GET으로 관찰 가능한 상태 변화 검증 |
| 3-3 | 존재하지 않는 categoryId로 생성 시도 | HTTP 5xx/4xx 에러 응답 (NoSuchElementException) |

### 행위 4: 상품 조회 (`GET /api/products`)

| TC | 시나리오 | 검증 항목 |
|----|---------|-----------|
| 4-1 | 상품이 없을 때 빈 목록 반환 | HTTP 200, 빈 배열 `[]` |

### 행위 5: 선물 전달 (`POST /api/gifts`)

| TC | 시나리오 | 검증 항목 |
|----|---------|-----------|
| 5-1 | 유효한 요청으로 선물 전달 성공 | HTTP 200 (사이드 이펙트: 재고 차감, GiftDelivery 호출) |
| 5-2 | 재고보다 많은 수량 요청 시 실패 | HTTP 5xx 에러 (IllegalStateException) |
| 5-3 | 존재하지 않는 optionId로 요청 시 실패 | HTTP 5xx 에러 (NoSuchElementException) |

---

## 4. 테스트 데이터 준비 (Fixture) 설계

TEST_STRATEGY.md에 따라 **Builder 패턴을 적용한 Fixture**로 데이터를 준비한다.

### 필요한 Fixture

| Fixture | 용도 | 의존 Repository |
|---------|------|-----------------|
| **CategoryFixture** | 카테고리 생성 | CategoryRepository |
| **ProductFixture** | 상품 생성 (Category 필요) | ProductRepository, CategoryRepository |
| **OptionFixture** | 옵션 생성 (Product 필요) — 선물 테스트 전용 | OptionRepository, ProductRepository |
| **MemberFixture** | 회원 생성 — 선물 테스트 전용 | MemberRepository |

```java
// 사용 예시
Category category = categoryFixture.builder().name("음료").build();
Product product = productFixture.builder().name("아메리카노").price(4500).category(category).build();
Option option = optionFixture.builder().name("ICE").quantity(10).product(product).build();
Member sender = memberFixture.builder().name("보내는사람").email("sender@test.com").build();
```

---

## 5. 테스트 구조

```
src/test/java/gift/
├── acceptance/
│   ├── category/
│   │   └── CategoryTest.java          # 행위 1, 2
│   ├── product/
│   │   └── ProductTest.java           # 행위 3, 4
│   └── gift/
│       └── GiftTest.java              # 행위 5
└── support/
    ├── DatabaseCleanup.java
    ├── CategoryFixture.java
    ├── ProductFixture.java
    ├── OptionFixture.java
    └── MemberFixture.java
```

---

## 6. 종합 정리

| 항목 | 수치 |
|------|------|
| 검증 대상 행위 수 | **5개** (TEST_STRATEGY.md 최소 기준 충족) |
| 총 테스트 케이스 수 | **8개** |
| 필요한 Fixture 수 | **4개** |
| 테스트 클래스 수 | **3개** |

### 핵심 참고사항

- `OptionService`, `WishService`는 컨트롤러가 없어 API 레벨 검증 불가 — Fixture로만 활용
- `CategoryRestController`, `ProductRestController`의 `create` 메서드에 `@RequestBody`가 없으므로, 테스트 시 JSON이 아닌 form parameter 방식으로 요청해야 함
- 선물 전달의 사이드 이펙트(재고 차감)는 직접 DB 조회 대신, 동일 옵션으로 재차 선물 시도 시 재고 부족 여부로 간접 검증 가능