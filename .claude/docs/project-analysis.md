# 프로젝트 분석

## 기술 스택

- Java 21, Gradle 8.4, Spring Boot 3.5.8
- Spring Data JPA + H2 인메모리 DB
- RestAssured (인수 테스트)
- `spring.jpa.open-in-view=false`

## 패키지 구조

```
gift/
├── ui/                  # REST 컨트롤러
│   ├── CategoryRestController   POST/GET /api/categories
│   ├── ProductRestController    POST/GET /api/products
│   └── GiftRestController       POST /api/gifts
├── application/         # 서비스 + 요청 DTO
│   ├── CategoryService, ProductService, OptionService, GiftService, WishService
│   └── CreateCategoryRequest, CreateProductRequest, CreateOptionRequest,
│       GiveGiftRequest, CreateWishRequest
├── model/               # JPA 엔티티 + Repository + 도메인 인터페이스
│   ├── Category, Product, Option, Member, Wish  (JPA 엔티티)
│   ├── Gift                                      (비영속 값 객체)
│   ├── GiftDelivery                              (인터페이스)
│   └── *Repository (Spring Data JPA)
└── infrastructure/      # 외부 연동
    ├── FakeGiftDelivery         GiftDelivery 구현체 (콘솔 출력)
    ├── KakaoMessageProperties
    └── KakaoSocialProperties
```

## 엔티티 관계

```
Category ←(ManyToOne)── Product ←(ManyToOne)── Option
                           ↑
Member ──(ManyToOne)── Wish ──(ManyToOne)──┘

Gift (비영속): from, to, option, quantity, message
```

- **Category**: id, name
- **Product**: id, name, price(int), imageUrl, category(ManyToOne)
- **Option**: id, name, quantity(int), product(ManyToOne) — `decrease(qty)` 메서드로 재고 차감
- **Member**: id, name, email (balance 필드 없음)
- **Wish**: id, member(ManyToOne), product(ManyToOne)
- **Gift**: 비영속 객체. JPA 엔티티 아님. 선물 정보를 GiftDelivery로 전달하는 용도

## API 엔드포인트

| 메서드 | 경로 | 요청 형식 | 응답 | 비고 |
|--------|------|----------|------|------|
| POST | `/api/categories` | JSON (`name`) | Category JSON | `@RequestBody` |
| GET | `/api/categories` | - | `List<Category>` JSON | |
| POST | `/api/products` | JSON (`name`, `price`, `imageUrl`, `categoryId`) | Product JSON + 중첩 Category | `@RequestBody` |
| GET | `/api/products` | - | `List<Product>` JSON + 중첩 Category | |
| POST | `/api/gifts` | JSON body + `Member-Id` 헤더 | void (빈 응답) | `@RequestBody` 있음 |

## 핵심 비즈니스 로직: 선물 보내기

```
GiftRestController.give(request, memberId)
  → GiftService.give(request, memberId)
    1. optionRepository.findById(optionId) — 없으면 NoSuchElementException
    2. option.decrease(quantity)            — 재고 부족 시 IllegalStateException
    3. Gift 객체 생성 (비영속)
    4. giftDelivery.deliver(gift)           — FakeGiftDelivery: 콘솔 출력
    * 예외 발생 시 @Transactional 롤백
```

## 현재 코드 특이사항

### 요청 형식
- 모든 POST 엔드포인트에 `@RequestBody` 적용 → JSON(`application/json`)으로 요청

### 예외 처리
- `Option.decrease()` → `IllegalStateException()` (메시지 없음)
- `Repository.findById().orElseThrow()` → `NoSuchElementException`
- 별도 `@ExceptionHandler` 없어 모두 HTTP 500으로 응답

### 미구현 기능
- 잔액/결제 (Member에 balance 필드 없음)
- 선물 취소/상태 전이 (Gift가 비영속, 상태 필드 없음)
- 선물 상태 조회 API

## 테스트 현황

### 테스트 인프라
- `DatabaseCleaner`: H2 TRUNCATE 기반 테스트 격리 (`@Component`)
- 모든 테스트: `@SpringBootTest(RANDOM_PORT)` + RestAssured
- `@BeforeEach`에서 `RestAssured.port` 설정 + `databaseCleaner.clear()` 호출
- 테스트 데이터는 Repository로 직접 삽입 (API 사용하지 않음)

### 테스트 파일 목록

| 파일 | 대상 행위 | 테스트 수 |
|------|----------|----------|
| `GiftAcceptanceTest` | POST /api/gifts | 4건 |
| `CategoryAcceptanceTest` | POST /api/categories | 1건 |
| `ProductAcceptanceTest` | POST /api/products | 2건 |
| `CategoryRetrieveAcceptanceTest` | GET /api/categories | 2건 |
| `ProductRetrieveAcceptanceTest` | GET /api/products | 2건 |

### 테스트 시나리오 요약

**선물하기 (Gift)**
- 정상 선물 보내기: 200 + 재고 차감 검증
- 재고 부족: 500 + 재고 불변 검증
- 재고 경계값(1개): 첫 번째 성공(qty→0), 두 번째 실패(qty 유지 0)
- 존재하지 않는 옵션: 500

**카테고리 등록**
- 정상 등록: 200 + 응답/DB 검증

**상품 등록**
- 정상 등록: 200 + 카테고리 연결 검증
- 존재하지 않는 카테고리: 500 + 상품 미생성 검증

**카테고리 조회**
- 빈 목록: 200 + `[]`
- 등록 후 조회: 200 + 2건 검증

**상품 조회**
- 빈 목록: 200 + `[]`
- 등록 후 조회: 200 + 2건 + 중첩 카테고리 검증

## 서비스 의존성

```
CategoryRestController → CategoryService → CategoryRepository
ProductRestController → ProductService → ProductRepository + CategoryRepository
GiftRestController    → GiftService    → OptionRepository + GiftDelivery(interface)
                                          FakeGiftDelivery(impl) → MemberRepository
```

- WishService, OptionService는 REST 엔드포인트 없음 (내부 사용 또는 미노출)
