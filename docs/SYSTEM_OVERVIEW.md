# 시스템 개요

## 이 서비스는 무엇인가

카카오톡 선물하기와 유사한 **선물 플랫폼**이다. 사용자가 상품의 옵션을 골라 다른 사용자에게 선물을 보낼 수 있다.

핵심 플로우는 하나다:

```
카테고리에 상품을 등록한다 → 상품에 옵션(사이즈, 수량)을 등록한다 → 옵션을 골라 선물한다 → 재고가 차감된다
```

선물 전달은 카카오톡 메시지 API를 통해 이루어질 예정이지만, 현재는 콘솔 출력으로 대체하는 Fake 구현만 존재한다.

## 기술 스택

| 항목 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.8 |
| 빌드 | Gradle 8.4 (wrapper) |
| 데이터베이스 | H2 (인메모리) |
| ORM | Spring Data JPA |
| 외부 연동 | 카카오톡 메시지 API (미구현) |

## 도메인 모델

### 엔티티 관계

```
Category ──< Product ──< Option
Member ──< Wish ──> Product
```

```
Category  "교환권"
    └── Product  "스타벅스 아메리카노" (4,500원)
            ├── Option  "Tall" (재고: 100)
            └── Option  "Grande" (재고: 50)

Member  "홍길동"
    └── Wish → Product  "스타벅스 아메리카노"
```

### 각 엔티티의 역할

**Category** — 상품 분류. name만 가진다.

**Product** — 선물할 수 있는 상품. 이름, 가격, 이미지를 가지며 하나의 Category에 속한다.

**Option** — 상품의 구매 단위. "Tall", "Grande" 같은 선택지이며 **재고(quantity)**를 관리한다. 이 프로젝트에서 유일하게 상태가 변하는 엔티티다. `decrease(quantity)` 메서드로 재고를 차감하며, 부족하면 `IllegalStateException`을 던진다.

**Member** — 서비스 사용자. 이름과 이메일을 가진다. 선물의 보내는 사람/받는 사람이 된다.

**Wish** — 회원의 위시리스트. Member와 Product의 다대다 관계를 중간 테이블로 풀어낸 것이다.

**Gift** — 선물 행위를 표현하는 값 객체. DB에 저장되지 않는다(`@Entity` 아님). 보내는 사람, 받는 사람, 옵션, 수량, 메시지를 담아 `GiftDelivery`에 전달된다.

## 패키지 구조

```
gift/
├── Application.java                          ← Spring Boot 진입점
│
├── ui/                                       ← 표현 계층 (REST Controller)
│   ├── CategoryRestController.java
│   ├── ProductRestController.java
│   └── GiftRestController.java
│
├── application/                              ← 응용 계층 (Service + DTO)
│   ├── CategoryService.java
│   ├── ProductService.java
│   ├── OptionService.java
│   ├── WishService.java
│   ├── GiftService.java
│   ├── CreateCategoryRequest.java
│   ├── CreateProductRequest.java
│   ├── CreateOptionRequest.java
│   ├── CreateWishRequest.java
│   └── GiveGiftRequest.java
│
├── model/                                    ← 도메인 계층 (Entity + Repository + Interface)
│   ├── Category.java, CategoryRepository.java
│   ├── Product.java, ProductRepository.java
│   ├── Option.java, OptionRepository.java
│   ├── Member.java, MemberRepository.java
│   ├── Wish.java, WishRepository.java
│   ├── Gift.java
│   └── GiftDelivery.java                    ← 배달 전략 인터페이스
│
└── infrastructure/                           ← 인프라 계층 (외부 연동 + 설정)
    ├── FakeGiftDelivery.java                 ← GiftDelivery의 Fake 구현
    ├── KakaoMessageProperties.java
    └── KakaoSocialProperties.java
```

### 계층 간 의존 방향

```
ui → application → model ← infrastructure
```

- `ui`는 `application`의 Service와 DTO만 안다.
- `application`은 `model`의 Entity와 Repository만 안다.
- `infrastructure`는 `model`의 인터페이스를 구현한다.
- **model은 아무것도 의존하지 않는다.** 가장 안쪽 계층이다.

`GiftDelivery` 인터페이스가 `model`에 있고 구현체인 `FakeGiftDelivery`가 `infrastructure`에 있는 것은 **의존성 역전(DIP)**이다. `GiftService`는 인터페이스에만 의존하므로, 배달 구현이 Fake에서 카카오 API로 바뀌어도 서비스 코드는 변경되지 않는다.

## API 엔드포인트

### 카테고리

| Method | Path | 요청 형식 | 설명 |
|--------|------|----------|------|
| POST | `/api/categories` | form param (`name`) | 카테고리 생성 |
| GET | `/api/categories` | - | 카테고리 목록 조회 |

### 상품

| Method | Path | 요청 형식 | 설명 |
|--------|------|----------|------|
| POST | `/api/products` | form param (`name`, `price`, `imageUrl`, `categoryId`) | 상품 생성 |
| GET | `/api/products` | - | 상품 목록 조회 |

### 선물

| Method | Path | 요청 형식 | 설명 |
|--------|------|----------|------|
| POST | `/api/gifts` | JSON body + `Member-Id` 헤더 | 선물하기 |

카테고리/상품은 `@RequestBody` 없이 form param으로 바인딩되고, 선물은 `@RequestBody`로 JSON 바인딩된다. 이 불일치는 의도적인 것인지 알 수 없으나 현재 동작에 영향을 준다.

## 핵심 플로우: 선물하기

이 서비스에서 가장 중요한 흐름이다.

```
클라이언트                     서버
   │                           │
   │  POST /api/gifts          │
   │  Header: Member-Id: 1     │
   │  Body: {                  │
   │    optionId: 5,           │
   │    quantity: 3,           │
   │    receiverId: 2,         │
   │    message: "축하해!"     │
   │  }                        │
   │ ─────────────────────────>│
   │                           │
   │              GiftRestController.give()
   │                    │
   │              GiftService.give()        ← @Transactional 시작
   │                    │
   │              optionRepository.findById(5)
   │                    │
   │              option.decrease(3)        ← 재고 100→97, 부족하면 예외
   │                    │
   │              new Gift(1, 2, option, 3, "축하해!")
   │                    │
   │              giftDelivery.deliver(gift) ← 카카오 메시지 전송 (현재는 콘솔 출력)
   │                    │
   │                           │            ← @Transactional 커밋 (재고 차감 반영)
   │  200 OK                   │
   │ <─────────────────────────│
```

`option.decrease()`에서 예외가 발생하면 트랜잭션이 롤백되어 재고 변경이 취소된다.

## 컨트롤러가 없는 서비스

`OptionService`와 `WishService`는 존재하지만 대응하는 REST 컨트롤러가 없다.

- **OptionService**: 옵션 CRUD. 현재 HTTP로 접근할 방법이 없다. 옵션 데이터는 DB에 직접 넣거나, 테스트에서 Repository로 시드해야 한다.
- **WishService**: 위시리스트 생성. 역시 HTTP 엔드포인트가 없다.

## 설정

`application.properties`:

```properties
spring.application.name=gift
spring.jpa.open-in-view=false
kakao.message.token=ACCESS_TOKEN
kakao.message.url=https://kapi.kakao.com/v1/api/talk
kakao.social.token=ACCESS_TOKEN
kakao.social.url=https://kapi.kakao.com/v1/api/talk
```

- **`open-in-view=false`**: JPA 지연 로딩이 `@Transactional` 범위 안에서만 동작한다. 컨트롤러에서 엔티티의 연관 객체에 접근하면 `LazyInitializationException`이 발생할 수 있다. 현재 코드에서는 `FakeGiftDelivery.deliver()`가 `option.getProduct()`를 호출하는데, 이는 `GiftService`의 `@Transactional` 안에서 실행되므로 문제없다.
- **카카오 API 설정**: `KakaoMessageProperties`와 `KakaoSocialProperties`로 바인딩된다. 토큰 값은 플레이스홀더 상태다.

## 현재 한계

| 항목 | 상태 |
|------|------|
| 인증/인가 | 없음. `Member-Id` 헤더를 신뢰한다 |
| 에러 핸들링 | `@ControllerAdvice` 없음. 모든 예외가 500으로 나간다 |
| 입력 검증 | Bean Validation 미사용. 잘못된 값이 그대로 DB까지 간다 |
| 카카오 연동 | `FakeGiftDelivery`로 콘솔 출력만 한다 |
| 옵션/위시 API | 서비스는 있으나 컨트롤러가 없다 |
| DTO setter | form param 바인딩용 setter가 누락되어 있다 |
