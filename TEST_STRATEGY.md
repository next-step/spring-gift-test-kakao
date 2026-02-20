# 테스트 전략 — 사용자 행위 분석

## 사용자가 할 수 있는 행위

### 1. 카테고리 생성

- **행위:** 카테고리 이름을 입력하여 새 카테고리를 만든다.
- **입력:** name (String)
- **정상 시나리오:**
  - 이름을 입력하면 카테고리가 생성되고 id가 부여된다.
- **예외 시나리오:**
  - (현재 검증 로직 없음 — name이 null/빈 문자열이어도 통과)

### 2. 카테고리 전체 조회

- **행위:** 등록된 모든 카테고리 목록을 조회한다.
- **정상 시나리오:**
  - 카테고리가 있으면 전체 목록 반환
  - 카테고리가 없으면 빈 리스트 반환

### 3. 상품 생성

- **행위:** 상품 정보를 입력하여 특정 카테고리에 속하는 상품을 만든다.
- **입력:** name (String), price (int), imageUrl (String), categoryId (Long)
- **정상 시나리오:**
  - 존재하는 카테고리에 상품을 등록하면 상품이 생성된다.
- **예외 시나리오:**
  - 존재하지 않는 categoryId → `NoSuchElementException`

### 4. 상품 전체 조회

- **행위:** 등록된 모든 상품 목록을 조회한다.
- **정상 시나리오:**
  - 상품이 있으면 전체 목록 반환 (카테고리 정보 포함)
  - 상품이 없으면 빈 리스트 반환

### 5. 옵션 생성 (컨트롤러 미노출)

- **행위:** 특정 상품에 옵션(색상, 사이즈 등)을 추가한다.
- **입력:** name (String), quantity (int), productId (Long)
- **정상 시나리오:**
  - 존재하는 상품에 옵션을 추가하면 재고(quantity)와 함께 생성된다.
- **예외 시나리오:**
  - 존재하지 않는 productId → `NoSuchElementException`

### 6. 옵션 전체 조회 (컨트롤러 미노출)

- **행위:** 등록된 모든 옵션 목록을 조회한다.

### 7. 위시리스트에 상품 추가 (컨트롤러 미노출)

- **행위:** 회원이 원하는 상품을 위시리스트에 담는다.
- **입력:** memberId (Long), productId (Long)
- **정상 시나리오:**
  - 존재하는 회원과 상품이면 위시가 생성된다.
- **예외 시나리오:**
  - 존재하지 않는 memberId → `NoSuchElementException`
  - 존재하지 않는 productId → `NoSuchElementException`

### 8. 선물 보내기

- **행위:** 회원이 다른 회원에게 특정 옵션의 상품을 선물한다.
- **입력:** optionId (Long), quantity (int), receiverId (Long), message (String) + Header: Member-Id (Long)
- **정상 시나리오:**
  - 재고가 충분하면 옵션 수량이 차감되고, GiftDelivery를 통해 선물이 전달된다.
- **예외 시나리오:**
  - 존재하지 않는 optionId → `NoSuchElementException`
  - 재고 부족 (quantity > 보유 수량) → `IllegalStateException`

## 핵심 비즈니스 로직 — 테스트 우선순위

| 우선순위 | 대상 | 이유 |
|---|---|---|
| **높음** | `Option.decrease()` | 재고 차감 + 부족 시 예외 — 유일한 도메인 규칙 |
| **높음** | `GiftService.give()` | 옵션 조회 → 재고 차감 → Gift 생성 → 배달의 흐름 |
| **중간** | `ProductService.create()` | 카테고리 존재 검증 후 상품 생성 |
| **중간** | `WishService.create()` | 회원+상품 존재 검증 후 위시 생성 |
| **낮음** | `CategoryService` | 단순 CRUD, 별도 검증 없음 |
| **낮음** | `OptionService.create()` | 상품 존재 검증 후 옵션 생성 (ProductService와 패턴 동일) |

## 인수 테스트 데이터 세팅 전략

### 비교

| 방식 | 장점 | 단점 |
|---|---|---|
| **SQL (`@Sql`, `data.sql`)** | 빠름, 앱 코드와 무관하게 동작 | DB 스키마 변경 시 SQL도 수정 필요, 내부 스키마에 직접 결합 |
| **JPA Repository 직접 주입** | 타입 안전, 리팩터링에 강함 | 내부 구현(엔티티/리포지토리)에 결합, API 레이어를 우회하여 버그를 놓칠 수 있음 |
| **API 호출 (POST)** | 풀스택 검증, 내부 구현과 무관 | 생성 API가 깨지면 조회 테스트도 같이 실패 (cascade) |

### 선택: API 호출 (POST)

인수 테스트는 **사용자 관점의 블랙박스 테스트**이므로, 데이터 세팅도 사용자가 실제로 하는 방식(API 호출)과 동일해야 한다.

- 내부 구현(스키마, 엔티티)에 결합하지 않아 리팩터링에 안전하다.
- 생성 API가 깨지면 생성 테스트가 먼저 실패하므로, cascade 실패의 원인 추적이 어렵지 않다.
- 이미 작성한 util 메서드(`createCategory` 등)를 재사용하여 데이터를 세팅한다.

## 테스트 레벨 가이드

| 레벨 | 대상 | 방식 |
|---|---|---|
| **단위 테스트** | `Option.decrease()` | 순수 Java 테스트, Spring 불필요 |
| **서비스 테스트** | `GiftService`, `ProductService`, `WishService` 등 | `@SpringBootTest` 또는 Mock 활용 |
| **API 테스트** | 컨트롤러 엔드포인트 | `MockMvc` 또는 `@SpringBootTest` + `TestRestTemplate` |
