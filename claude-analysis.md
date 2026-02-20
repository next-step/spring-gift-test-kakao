# 비즈니스 로직 분석

## 1. 아키텍처 개요

```
ui (Controller)  →  application (Service + Request DTO)  →  model (Entity + Repository)
                                                          →  infrastructure (외부 연동 구현체)
```

- 전형적인 레이어드 아키텍처
- Controller → Service → Repository 흐름
- `GiftDelivery` 인터페이스를 통해 선물 전달 기능을 추상화 (현재는 `FakeGiftDelivery`로 콘솔 출력)

---

## 2. 엔티티 관계도

```
Category (1) ←── (N) Product (1) ←── (N) Option
                       ↑                    ↑
                       |                    |
                  Wish (N:1)          Gift (비영속 객체, Option 참조)
                       |
                       ↓
                  Member (1) ←── (N) Wish
```

| 엔티티 | 필드 | 비고 |
|--------|------|------|
| **Category** | id, name | |
| **Product** | id, name, price(int), imageUrl, category(ManyToOne) | price 검증 로직 없음 |
| **Option** | id, name, quantity(int), product(ManyToOne) | `decrease()` 메서드에서 재고 차감 + 부족 시 예외 |
| **Member** | id, name, email | |
| **Wish** | id, member(ManyToOne), product(ManyToOne) | |
| **Gift** | from, to, option, quantity, message | **비영속 객체** (`@Entity` 아님) |

---

## 3. API 엔드포인트 분석

### 3.1 상품 API (`/api/products`)

| Method | Path | 설명 | Request | Response |
|--------|------|------|---------|----------|
| POST | `/api/products` | 상품 추가 | `CreateProductRequest` (폼 바인딩) | `Product` |
| GET | `/api/products` | 전체 상품 조회 | - | `List<Product>` |

**비즈니스 로직 (`ProductService.create`)**:
1. `categoryId`로 Category를 조회한다. → 없으면 `NoSuchElementException`
2. `new Product(name, price, imageUrl, category)`로 상품을 생성한다.
3. Repository에 저장 후 반환한다.

**주의사항**:
- Controller에서 `@RequestBody` 없이 파라미터를 받고 있음 → **폼 데이터 바인딩** 방식
- `price`에 대한 유효성 검증이 **전혀 없음** (음수 가능)
- `categoryId`가 null이면 `findById(null)` → `IllegalArgumentException` 발생

---

### 3.2 카테고리 API (`/api/categories`)

| Method | Path | 설명 | Request | Response |
|--------|------|------|---------|----------|
| POST | `/api/categories` | 카테고리 추가 | `CreateCategoryRequest` (폼 바인딩) | `Category` |
| GET | `/api/categories` | 전체 카테고리 조회 | - | `List<Category>` |

**비즈니스 로직 (`CategoryService.create`)**:
1. `new Category(name)`으로 카테고리를 생성한다.
2. Repository에 저장 후 반환한다.

**주의사항**:
- `@RequestBody` 없음 → 폼 데이터 바인딩 방식
- name 유효성 검증 없음

---

### 3.3 선물 API (`/api/gifts`)

| Method | Path | 설명 | Request | Response |
|--------|------|------|---------|----------|
| POST | `/api/gifts` | 선물 보내기 | `GiveGiftRequest` (JSON body) + `Member-Id` (헤더) | void |

**비즈니스 로직 (`GiftService.give`)**:
1. `optionId`로 Option을 조회한다. → 없으면 `NoSuchElementException`
2. `option.decrease(quantity)` 호출로 재고를 차감한다.
   - 재고 < 요청 수량 → `IllegalStateException` 발생
3. `Gift` 객체를 생성한다 (from=memberId, to=receiverId, option, quantity, message).
4. `giftDelivery.deliver(gift)`를 호출한다.
   - `FakeGiftDelivery`에서 `memberId`로 Member 조회 → 없으면 `NoSuchElementException`
   - 콘솔에 정보 출력

**주의사항**:
- `@RequestBody`를 사용하여 JSON으로 요청을 받음 (상품/카테고리와 다른 방식)
- `@RequestHeader("Member-Id")`로 회원 ID를 받음
- `receiverId` 유효성 검증 없음 (존재하지 않는 회원에게도 선물 가능)
- `memberId`는 GiftService가 아닌 **FakeGiftDelivery**에서 검증됨 (delivery 단계에서 조회)

---

## 4. 컨트롤러에 노출되지 않은 서비스

### 4.1 OptionService

**엔드포인트 없음** — Controller가 존재하지 않는다.

```java
public Option create(CreateOptionRequest request)  // productId로 Product 조회 후 Option 생성
public List<Option> retrieve()                      // 전체 Option 조회
```

- 테스트 시 사전 데이터로 Option을 만들려면 Repository를 직접 사용해야 함

### 4.2 WishService

**엔드포인트 없음** — Controller가 존재하지 않는다.

```java
public Wish create(Long memberId, CreateWishRequest request)  // memberId + productId로 Wish 생성
```

---

## 5. 인프라스트럭처 계층

### FakeGiftDelivery

- `GiftDelivery` 인터페이스의 구현체
- 실제 카카오 메시지 전송 대신 **콘솔 출력**
- `deliver()` 내부에서 `memberRepository.findById(gift.getFrom())`으로 발신자를 조회
  - 이 시점에서 memberId가 유효하지 않으면 예외 발생

---

## 6. 레거시 코드 특징 (리팩토링 포인트)

### 6.1 일관성 부재
- **요청 바인딩 방식 불일치**: Product/Category는 폼 바인딩, Gift는 `@RequestBody`(JSON)
- OptionService, WishService에 대응하는 Controller가 없음

### 6.2 유효성 검증 부재
- `Product.price`에 대한 검증 없음 (음수 가능)
- `Category.name`, `Product.name` 등에 대한 null/빈 문자열 검증 없음
- `GiveGiftRequest`의 `receiverId` 존재 여부를 검증하지 않음

### 6.3 예외 처리 미흡
- `orElseThrow()`로 던지는 `NoSuchElementException`에 대한 글로벌 예외 핸들러 없음
- `Option.decrease()`의 `IllegalStateException`에 메시지 없음
- 클라이언트에게 의미 있는 에러 응답을 제공하지 못함

### 6.4 책임 분리 문제
- `FakeGiftDelivery`에서 발신자(Member) 조회를 수행 → 이 검증이 Service 계층에 있어야 할 수 있음
- `Gift` 객체가 비영속이지만 memberId(Long)를 들고 있어, delivery 시점에 다시 Member를 조회해야 함

### 6.5 응답 설계
- Entity를 그대로 Response로 반환 → 순환 참조, 불필요한 필드 노출 가능성
- POST 응답에 HTTP 상태 코드가 200 (201 Created가 아님)

---

## 7. 테스트 관점 핵심 정리

인수 테스트 작성 시 주목해야 할 **핵심 흐름**:

| # | 행위 | 핵심 검증 포인트 |
|---|------|-----------------|
| 1 | 상품 추가 | 정상 생성 + 응답 확인 |
| 2 | 상품 조회 | 저장된 상품이 조회되는지 |
| 3 | 상품 추가 후 조회 | 생성 → 조회 시나리오 연결 |
| 4 | 상품 추가 시 카테고리 없음 | categoryId null 또는 미존재 → 에러 |
| 5 | 상품 price 음수 | 현재 검증 없음 → 어떻게 테스트할 것인가? |
| 6 | 카테고리 추가 | 정상 생성 + 응답 확인 |
| 7 | 카테고리 조회 | 저장된 카테고리가 조회되는지 |
| 8 | 카테고리 추가 후 조회 | 생성 → 조회 시나리오 연결 |
| 9 | 선물 보내기 | 정상 선물 전달 + 재고 차감 확인 |
| 10 | 선물 - optionId/receiverId 미존재 | 에러 응답 확인 |
| 11 | 선물 - Member-Id 헤더 미존재 | 에러 응답 확인 |
| 12 | 선물 - 재고 부족 | IllegalStateException → 에러 응답 |
