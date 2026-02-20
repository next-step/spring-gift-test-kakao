# Spring Gift 프로젝트 분석

## 1. 프로젝트 개요

Spring Boot 기반의 **선물 관리 시스템**으로, 사용자가 상품을 등록하고 다른 회원에게 선물을 전달할 수 있는 서비스입니다.

### 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 | 언어 |
| Spring Boot | 3.5.8 | 프레임워크 |
| Spring Data JPA | - | ORM/데이터 접근 |
| H2 Database | - | 인메모리 DB |
| Thymeleaf | - | 템플릿 엔진 |

---

## 2. 프로젝트 구조

```
src/main/java/gift/
├── Application.java              # 메인 클래스
├── ui/                           # 프레젠테이션 계층
│   ├── ProductRestController.java
│   ├── CategoryRestController.java
│   └── GiftRestController.java
├── application/                  # 애플리케이션 계층
│   ├── ProductService.java
│   ├── CategoryService.java
│   ├── OptionService.java
│   ├── GiftService.java
│   ├── WishService.java
│   └── *Request.java             # DTO 클래스들
├── model/                        # 도메인 계층
│   ├── Product.java
│   ├── Category.java
│   ├── Option.java
│   ├── Member.java
│   ├── Gift.java
│   ├── Wish.java
│   ├── GiftDelivery.java         # 인터페이스
│   └── *Repository.java          # 리포지토리 인터페이스들
└── infrastructure/               # 인프라 계층
    ├── FakeGiftDelivery.java
    ├── KakaoMessageProperties.java
    └── KakaoSocialProperties.java
```

### 계층 구조

```
┌─────────────────────────────────────┐
│           UI 계층                   │
│    (RestController)                 │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│       Application 계층              │
│    (Service, DTO)                   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│         Model 계층                  │
│    (Entity, Repository)             │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│      Infrastructure 계층            │
│    (외부 서비스 연동)                │
└─────────────────────────────────────┘
```

---

## 3. 도메인 모델

### 엔티티 관계도 (ERD)

```
┌─────────────┐
│  Category   │
└──────┬──────┘
       │ 1:N
       ▼
┌─────────────┐
│   Product   │
└──────┬──────┘
       │ 1:N
       ▼
┌─────────────┐
│   Option    │
└─────────────┘

┌─────────────┐       ┌─────────────┐
│   Member    │──M:N──│   Product   │
└─────────────┘       └─────────────┘
       │
       └─── Wish (조인 테이블)
```

### 엔티티 상세

#### Category (카테고리)
```java
@Entity
public class Category {
    private Long id;
    private String name;
}
```

#### Product (상품)
```java
@Entity
public class Product {
    private Long id;
    private String name;
    private int price;
    private String imageUrl;

    @ManyToOne
    private Category category;
}
```

#### Option (옵션)
```java
@Entity
public class Option {
    private Long id;
    private String name;
    private int quantity;    // 재고 수량

    @ManyToOne
    private Product product;

    public void decrease(int quantity);  // 재고 감소
}
```

#### Member (회원)
```java
@Entity
public class Member {
    private Long id;
    private String name;
    private String email;
}
```

#### Wish (위시리스트)
```java
@Entity
public class Wish {
    private Long id;

    @ManyToOne
    private Member member;

    @ManyToOne
    private Product product;
}
```

#### Gift (선물 정보 - Value Object)
```java
public class Gift {
    private final Long from;       // 보내는 사람 ID
    private final Long to;         // 받는 사람 ID
    private final Option option;
    private final int quantity;
    private final String message;
}
```

---

## 4. API 엔드포인트

### 현재 사용 가능한 API

| 메서드 | 경로 | 설명 | 요청 바디 |
|--------|------|------|----------|
| POST | `/api/categories` | 카테고리 생성 | `{ "name": "..." }` |
| GET | `/api/categories` | 카테고리 목록 조회 | - |
| POST | `/api/products` | 상품 등록 | `{ "name", "price", "imageUrl", "categoryId" }` |
| GET | `/api/products` | 상품 목록 조회 | - |
| POST | `/api/gifts` | 선물 전달 | `{ "optionId", "quantity", "receiverId", "message" }` |

### 선물 전달 API 상세

```http
POST /api/gifts
Header: Member-Id: {보내는 사람 ID}
Content-Type: application/json

{
    "optionId": 1,
    "quantity": 2,
    "receiverId": 3,
    "message": "생일 축하해!"
}
```

### 미구현 API (서비스만 존재)

| 기능 | 서비스 | 상태 |
|------|--------|------|
| 옵션 생성/조회 | `OptionService` | 컨트롤러 없음 |
| 위시리스트 추가 | `WishService` | 컨트롤러 없음 |
| 회원 관리 | - | 서비스/컨트롤러 없음 |

---

## 5. 핵심 비즈니스 로직

### 재고 변경 흐름

선물 전달 시 재고가 감소하는 전체 흐름입니다.

```
HTTP Request (POST /api/gifts)
         │
         ▼
┌─────────────────────────────────────┐
│  GiftRestController.give()          │
│  └─ giftService.give(request, id)   │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  GiftService.give()                 │
│  @Transactional                     │
│  ┌─────────────────────────────────┐│
│  │ ① optionRepository.findById()  ││
│  │    → Option 조회                ││
│  │                                 ││
│  │ ② option.decrease(quantity)    ││
│  │    → 재고 감소                  ││
│  │                                 ││
│  │ ③ new Gift(...)                ││
│  │    → Gift 객체 생성             ││
│  │                                 ││
│  │ ④ giftDelivery.deliver(gift)   ││
│  │    → 선물 배달                  ││
│  └─────────────────────────────────┘│
│  트랜잭션 커밋 → UPDATE 쿼리 실행    │
└─────────────────────────────────────┘
```

### Option.decrease() 메서드

```java
public void decrease(final int quantity) {
    if (this.quantity < quantity) {
        throw new IllegalStateException();  // 재고 부족
    }
    this.quantity -= quantity;  // 재고 차감
}
```

### 재고 변경 시나리오

| 상황 | 현재 재고 | 요청 수량 | 결과 |
|------|----------|----------|------|
| 정상 | 10 | 2 | 재고 8, 성공 |
| 재고 부족 | 1 | 5 | 예외 발생, 롤백 |
| 재고 소진 | 10 | 10 | 재고 0, 성공 |

---

## 6. 설계 패턴

| 패턴 | 적용 위치 | 효과 |
|------|----------|------|
| **Strategy** | `GiftDelivery` 인터페이스 | 배달 수단 교체 가능 |
| **Repository** | `*Repository` 인터페이스 | 데이터 접근 추상화 |
| **DTO** | `*Request` 클래스 | 계층 간 데이터 전달 |
| **DI** | 생성자 주입 | 느슨한 결합 |

### GiftDelivery 전략 패턴

```
       ┌─────────────────┐
       │  GiftDelivery   │  ◄── 인터페이스
       │  + deliver()    │
       └────────┬────────┘
                │
       ┌────────┴────────┐
       │                 │
       ▼                 ▼
┌──────────────┐  ┌──────────────┐
│FakeGiftDeli- │  │KakaoGiftDeli-│
│very (구현됨)  │  │very (미구현)  │
└──────────────┘  └──────────────┘
```

---

## 7. 설정

### application.properties

```properties
spring.application.name=gift
spring.jpa.open-in-view=false

# 카카오 API 설정
kakao.message.token=ACCESS_TOKEN
kakao.message.url=https://kapi.kakao.com/v1/api/talk
kakao.social.token=ACCESS_TOKEN
kakao.social.url=https://kapi.kakao.com/v1/api/talk
```

### 주요 설정 설명

| 설정 | 값 | 설명 |
|------|-----|------|
| `spring.jpa.open-in-view` | false | 컨트롤러에서 Lazy Loading 비활성화 |
| `kakao.message.*` | - | 카카오 메시지 API 인증 정보 |
| `kakao.social.*` | - | 카카오 소셜 API 인증 정보 |

---

## 8. 확장 가능성

### 현재 상태
- 기본 CRUD 기능 구현
- 카카오 API 연동 준비 (Properties 설정 완료)
- 테스트용 `FakeGiftDelivery` 구현

### 향후 개선 사항

1. **API 확장**
   - 옵션 관리 API 추가
   - 위시리스트 API 추가
   - 회원 관리 API 추가
   - 선물 이력 조회 API 추가

2. **카카오 연동**
   - 실제 카카오 메시지 API 호출 구현
   - 카카오 소셜 로그인 연동

3. **검증 강화**
   - DTO 유효성 검증 (@NotNull, @NotBlank 등)
   - 커스텀 예외 처리
   - 에러 응답 정규화

4. **보안**
   - Spring Security 적용
   - JWT 인증
   - 권한 관리

5. **성능**
   - N+1 쿼리 최적화
   - 캐싱 적용
   - 페이징 처리
