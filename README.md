# Spring Gift

Spring Boot 기반의 선물하기 서비스 API 서버입니다. 카테고리별 상품 관리, 위시리스트, 선물하기 기능을 제공합니다.

## 프로젝트 개요

| 항목 | 내용 |
|---|---|
| Framework | Spring Boot 3.5.8 |
| Build Tool | Gradle 8.4 |
| Java | 25 |
| Database | H2 (인메모리) |
| 아키텍처 | 계층형 (UI → Application → Model → Infrastructure) |

### 의존성

| 의존성 | 용도 |
|---|---|
| `spring-boot-starter-data-jpa` | JPA 기반 데이터 접근 |
| `spring-boot-starter-thymeleaf` | 템플릿 엔진 (현재 미사용) |
| `spring-boot-starter-web` | REST API 서버 |
| `com.h2database:h2` | 인메모리 데이터베이스 |
| `spring-boot-starter-test` | 테스트 (현재 미작성) |

---

## 프로젝트 구조

```
spring-gift-test/
├── build.gradle                        # Gradle 빌드 설정
├── settings.gradle                     # Gradle 프로젝트 설정
├── gradlew / gradlew.bat              # Gradle Wrapper
├── gradle/wrapper/
│   └── gradle-wrapper.properties      # Wrapper 버전 설정
└── src/
    ├── main/
    │   ├── java/gift/
    │   │   ├── Application.java                        # 애플리케이션 진입점
    │   │   ├── model/                                  # 도메인 계층
    │   │   │   ├── Category.java                       # 카테고리 엔티티
    │   │   │   ├── CategoryRepository.java             # 카테고리 리포지토리
    │   │   │   ├── Product.java                        # 상품 엔티티
    │   │   │   ├── ProductRepository.java              # 상품 리포지토리
    │   │   │   ├── Option.java                         # 상품 옵션 엔티티 (재고 관리)
    │   │   │   ├── OptionRepository.java               # 옵션 리포지토리
    │   │   │   ├── Member.java                         # 회원 엔티티
    │   │   │   ├── MemberRepository.java               # 회원 리포지토리
    │   │   │   ├── Wish.java                           # 위시리스트 엔티티
    │   │   │   ├── WishRepository.java                 # 위시리스트 리포지토리
    │   │   │   ├── Gift.java                           # 선물 도메인 객체 (비영속)
    │   │   │   └── GiftDelivery.java                   # 선물 전달 포트 인터페이스
    │   │   ├── application/                            # 애플리케이션 계층
    │   │   │   ├── CategoryService.java                # 카테고리 비즈니스 로직
    │   │   │   ├── ProductService.java                 # 상품 비즈니스 로직
    │   │   │   ├── OptionService.java                  # 옵션 비즈니스 로직
    │   │   │   ├── WishService.java                    # 위시리스트 비즈니스 로직
    │   │   │   ├── GiftService.java                    # 선물하기 비즈니스 로직
    │   │   │   ├── CreateCategoryRequest.java          # 카테고리 생성 요청 DTO
    │   │   │   ├── CreateProductRequest.java           # 상품 생성 요청 DTO
    │   │   │   ├── CreateOptionRequest.java            # 옵션 생성 요청 DTO
    │   │   │   ├── CreateWishRequest.java              # 위시리스트 추가 요청 DTO
    │   │   │   └── GiveGiftRequest.java                # 선물하기 요청 DTO
    │   │   ├── infrastructure/                         # 인프라스트럭처 계층
    │   │   │   ├── FakeGiftDelivery.java               # GiftDelivery 구현체 (Fake, stdout 출력)
    │   │   │   ├── KakaoMessageProperties.java         # 카카오 메시지 API 설정
    │   │   │   └── KakaoSocialProperties.java          # 카카오 소셜 API 설정
    │   │   └── ui/                                     # UI(프레젠테이션) 계층
    │   │       ├── CategoryRestController.java         # 카테고리 REST API
    │   │       ├── ProductRestController.java          # 상품 REST API
    │   │       └── GiftRestController.java             # 선물하기 REST API
    │   └── resources/
    │       └── application.properties                  # 애플리케이션 설정
    └── test/
        └── java/gift/
            └── .gitkeep                                # 테스트 디렉토리 placeholder
```

---

## 아키텍처

4계층 구조와 Ports & Adapters 패턴을 혼합하여 사용합니다.

```
┌─────────────────────────────────────────┐
│               UI 계층 (ui/)             │
│   CategoryRestController                │
│   ProductRestController                 │
│   GiftRestController                    │
│                  │                      │
│                  ▼                      │
├─────────────────────────────────────────┤
│         Application 계층 (application/) │
│   CategoryService, ProductService       │
│   OptionService, WishService            │
│   GiftService                           │
│   Request DTOs                          │
│                  │                      │
│                  ▼                      │
├─────────────────────────────────────────┤
│           Model 계층 (model/)           │
│   Entity: Category, Product, Option,    │
│           Member, Wish                  │
│   Domain Object: Gift                   │
│   Port Interface: GiftDelivery          │
│   Repository Interfaces                 │
│                  ▲                      │
│                  │ (implements)          │
├─────────────────────────────────────────┤
│     Infrastructure 계층 (infrastructure/)│
│   FakeGiftDelivery (GiftDelivery 구현)  │
│   KakaoMessageProperties               │
│   KakaoSocialProperties                │
└─────────────────────────────────────────┘
```

### 계층별 책임

| 계층 | 패키지 | 책임 |
|---|---|---|
| **UI** | `gift.ui` | HTTP 요청/응답 처리, 컨트롤러 |
| **Application** | `gift.application` | 비즈니스 유스케이스 조율, 트랜잭션 관리 |
| **Model** | `gift.model` | 도메인 엔티티, 리포지토리 인터페이스, 도메인 로직 |
| **Infrastructure** | `gift.infrastructure` | 외부 시스템 연동 구현체, 설정 |

### 의존 방향

- `UI → Application → Model ← Infrastructure`
- Model 계층은 다른 계층에 의존하지 않습니다.
- Infrastructure는 Model의 인터페이스(`GiftDelivery`)를 구현합니다 (DIP).

---

## 도메인 모델 & 엔티티 관계

### ER 다이어그램

```
Category (1) ────< Product (1) ────< Option
                       │
                       │ (N)
                       │
Member (1) ─────< Wish ┘

Gift (비영속 도메인 객체, DB 저장 안 됨)
  - from: Long (보내는 회원 ID)
  - to: Long (받는 회원 ID)
  - option: Option
  - quantity: int
  - message: String
```

### 엔티티 상세

#### Category

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | PK, 자동 생성 (IDENTITY) |
| `name` | `String` | 카테고리명 |

#### Product

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | PK, 자동 생성 (IDENTITY) |
| `name` | `String` | 상품명 |
| `price` | `int` | 가격 |
| `imageUrl` | `String` | 상품 이미지 URL |
| `category` | `Category` | `@ManyToOne` - 소속 카테고리 |

#### Option

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | PK, 자동 생성 (IDENTITY) |
| `name` | `String` | 옵션명 |
| `quantity` | `int` | 재고 수량 |
| `product` | `Product` | `@ManyToOne` - 소속 상품 |

도메인 로직:
- `decrease(int quantity)` - 재고 차감. 재고 부족 시 `IllegalStateException` 발생.

#### Member

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | PK, 자동 생성 (IDENTITY) |
| `name` | `String` | 회원명 |
| `email` | `String` | 이메일 |

#### Wish

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | PK, 자동 생성 (IDENTITY) |
| `member` | `Member` | `@ManyToOne` - 회원 |
| `product` | `Product` | `@ManyToOne` - 상품 |

#### Gift (비영속 도메인 객체)

`@Entity`가 아닌 일반 클래스입니다. 선물하기 트랜잭션 중 메모리에서만 사용됩니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `from` | `Long` | 보내는 회원 ID |
| `to` | `Long` | 받는 회원 ID |
| `option` | `Option` | 선물 옵션 |
| `quantity` | `int` | 수량 |
| `message` | `String` | 선물 메시지 |

---

## API 엔드포인트

### 카테고리 API

| Method | Path | Request | Response | 설명 |
|---|---|---|---|---|
| `POST` | `/api/categories` | Form: `name` | `Category` (JSON) | 카테고리 생성 |
| `GET` | `/api/categories` | - | `List<Category>` (JSON) | 전체 카테고리 조회 |

> **참고:** `@RequestBody` 없이 바인딩되므로 Form/Query Parameter 방식입니다.

### 상품 API

| Method | Path | Request | Response | 설명 |
|---|---|---|---|---|
| `POST` | `/api/products` | Form: `name`, `price`, `imageUrl`, `categoryId` | `Product` (JSON) | 상품 생성 |
| `GET` | `/api/products` | - | `List<Product>` (JSON) | 전체 상품 조회 |

> **참고:** 카테고리와 동일하게 Form/Query Parameter 방식입니다.

### 선물하기 API

| Method | Path | Request | Header | Response | 설명 |
|---|---|---|---|---|---|
| `POST` | `/api/gifts` | Body(JSON): `optionId`, `quantity`, `receiverId`, `message` | `Member-Id: {memberId}` | `void` (200) | 선물하기 |

### 요청/응답 예시

#### 카테고리 생성

```bash
# 요청
curl -X POST "http://localhost:8080/api/categories?name=식품"

# 응답
{
  "id": 1,
  "name": "식품"
}
```

#### 상품 생성

```bash
# 요청
curl -X POST "http://localhost:8080/api/products?name=아메리카노&price=4500&imageUrl=https://example.com/coffee.jpg&categoryId=1"

# 응답
{
  "id": 1,
  "name": "아메리카노",
  "price": 4500,
  "imageUrl": "https://example.com/coffee.jpg",
  "category": {
    "id": 1,
    "name": "식품"
  }
}
```

#### 전체 상품 조회

```bash
# 요청
curl http://localhost:8080/api/products

# 응답
[
  {
    "id": 1,
    "name": "아메리카노",
    "price": 4500,
    "imageUrl": "https://example.com/coffee.jpg",
    "category": {
      "id": 1,
      "name": "식품"
    }
  }
]
```

#### 선물하기

```bash
# 요청
curl -X POST http://localhost:8080/api/gifts \
  -H "Content-Type: application/json" \
  -H "Member-Id: 1" \
  -d '{
    "optionId": 1,
    "quantity": 2,
    "receiverId": 2,
    "message": "생일 축하합니다!"
  }'

# 응답: 200 OK (본문 없음)
```

### 미구현 API (서비스만 존재, 컨트롤러 없음)

| 기능 | 서비스 | 요청 DTO | 비고 |
|---|---|---|---|
| 옵션 생성 | `OptionService.create()` | `CreateOptionRequest` (`name`, `quantity`, `productId`) | 컨트롤러 미구현 |
| 위시리스트 추가 | `WishService.create()` | `CreateWishRequest` (`productId`) + `memberId` 파라미터 | 컨트롤러 미구현 |

---

## User Flow

### 1. 정상 흐름: 카테고리 생성 → 상품 등록 → 옵션 추가 → 선물하기

```
1. POST /api/categories?name=음료
   → Category 생성 (id: 1)

2. POST /api/products?name=아메리카노&price=4500&imageUrl=...&categoryId=1
   → Product 생성 (id: 1, category: 음료)

3. [미구현] 옵션 생성 API 호출
   → Option 생성 (id: 1, name: "Tall", quantity: 100, product: 아메리카노)
   ※ 현재 DB에 직접 INSERT하거나 OptionService를 직접 호출해야 합니다.

4. POST /api/gifts (Header: Member-Id: 1)
   Body: { "optionId": 1, "quantity": 2, "receiverId": 2, "message": "선물!" }
   → Option 재고 100 → 98로 차감
   → FakeGiftDelivery가 stdout에 정보 출력
```

### 2. 위시리스트 추가 흐름 (컨트롤러 미구현)

```
1. Member, Product가 DB에 존재하는 상태에서
2. WishService.create(memberId, new CreateWishRequest(productId)) 호출
   → Wish 엔티티 생성 및 저장
※ REST API로는 아직 호출할 수 없습니다.
```

### 3. 에러 케이스

#### 존재하지 않는 카테고리로 상품 등록

```
POST /api/products?name=상품&price=1000&imageUrl=...&categoryId=999
→ CategoryRepository.findById(999) → NoSuchElementException
→ 500 Internal Server Error (에러 핸들러 미구현)
```

#### 재고 부족 선물하기

```
POST /api/gifts (optionId: 1, quantity: 1000)
→ Option.decrease(1000) → IllegalStateException("재고 부족")
→ 500 Internal Server Error (에러 핸들러 미구현)
```

#### 존재하지 않는 옵션/상품/회원 조회

```
서비스에서 findById().orElseThrow() 호출
→ NoSuchElementException
→ 500 Internal Server Error (에러 핸들러 미구현)
```

---

## 설정 정보

### application.properties

```properties
spring.application.name=gift                              # 애플리케이션 이름
spring.jpa.open-in-view=false                             # OSIV 비활성화 (권장 설정)
kakao.message.token=ACCESS_TOKEN                          # 카카오 메시지 API 토큰 (placeholder)
kakao.message.url=https://kapi.kakao.com/v1/api/talk      # 카카오 메시지 API URL
kakao.social.token=ACCESS_TOKEN                           # 카카오 소셜 API 토큰 (placeholder)
kakao.social.url=https://kapi.kakao.com/v1/api/talk       # 카카오 소셜 API URL
```

### 주요 설정 사항

- **H2 인메모리 DB**: `spring.datasource.*` 미설정 → Spring Boot 자동 설정으로 인메모리 H2 사용. 서버 재시작 시 데이터 초기화됨.
- **OSIV 비활성화**: `spring.jpa.open-in-view=false` 설정으로 Controller에서의 지연 로딩 방지.
- **카카오 API**: `KakaoMessageProperties`, `KakaoSocialProperties`에 바인딩되지만 현재 어디서도 사용하지 않음. `FakeGiftDelivery`가 실제 API 호출 대신 stdout 출력.
- **`@ConfigurationPropertiesScan`**: `Application.java`에 선언되어 `@ConfigurationProperties` 빈 자동 등록.

---

## 실행 방법

### 요구사항

- Java 25 이상
- Gradle 8.4 이상 (Gradle Wrapper 포함)

### 빌드 & 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 또는 JAR 직접 실행
java -jar build/libs/spring-gift-test-0.0.1-SNAPSHOT.jar
```

서버는 `http://localhost:8080`에서 실행됩니다.

### H2 콘솔 접속

`application.properties`에 아래 설정을 추가하면 H2 웹 콘솔을 사용할 수 있습니다:

```properties
spring.h2.console.enabled=true
```

접속 URL: `http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:mem:testdb` (Spring Boot 기본값)

---

## 현재 한계점 및 향후 개선 필요사항

### 미구현 기능

| 항목 | 상세 |
|---|---|
| **OptionRestController** | `OptionService`는 구현되어 있으나 REST 엔드포인트가 없음. 옵션 CRUD API 추가 필요. |
| **WishRestController** | `WishService`는 구현되어 있으나 REST 엔드포인트가 없음. 위시리스트 API 추가 필요. |
| **에러 처리** | `@ControllerAdvice` / `@ExceptionHandler` 미구현. 모든 예외가 500 응답으로 반환됨. |
| **인증/인가** | Spring Security 미적용. `Member-Id` 헤더를 통한 임시 회원 식별만 사용 중. |
| **테스트** | 단위 테스트, 통합 테스트 모두 미작성. |
| **입력 검증** | `spring-boot-starter-validation` 미포함. `@Valid`, `@NotBlank` 등 요청 검증 없음. |
| **페이지네이션** | 전체 조회 API에 `Pageable` 미적용. 데이터 증가 시 성능 문제 발생 가능. |
| **실제 카카오 연동** | `FakeGiftDelivery`가 stdout 출력만 수행. 실제 카카오 메시지 API 연동 필요. |

### 코드 레벨 이슈

| 항목 | 상세 |
|---|---|
| **`@RequestBody` 불일치** | `CategoryRestController`, `ProductRestController`는 Form 파라미터 방식인 반면 `GiftRestController`는 JSON `@RequestBody` 사용. 통일 필요. |
| **응답 DTO 부재** | 엔티티를 직접 JSON 응답으로 반환 중. 응답 DTO를 분리하여 내부 구조 노출 방지 필요. |
| **Member 생성 API 부재** | Member 엔티티와 리포지토리는 있으나 회원 가입/조회 API 없음. |
| **Thymeleaf 미사용** | 의존성에 포함되어 있으나 템플릿이 없음. 불필요하면 제거 권장. |
