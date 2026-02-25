# 프로젝트 분석 문서

카카오 메시지 API를 활용한 선물하기 서비스 (Spring Boot)

## 기술 스택

| 항목 | 버전/내용 |
|------|----------|
| Java | 21 |
| Spring Boot | 3.5.8 |
| Build Tool | Gradle 8.4 |
| ORM | Spring Data JPA (Hibernate) |
| Database | H2 (인메모리) |
| Template Engine | Thymeleaf (의존성만 존재, 미사용) |
| 테스트 | spring-boot-starter-test (JUnit 5) |

## 패키지 구조 및 계층

```
gift/
├── Application.java              # @SpringBootApplication + @ConfigurationPropertiesScan
├── ui/                            # Presentation 계층 — REST 컨트롤러
├── application/                   # Application 계층 — 서비스 + 요청 DTO
├── model/                         # Domain 계층 — 엔티티, 리포지토리, 도메인 인터페이스
└── infrastructure/                # Infrastructure 계층 — 외부 연동 구현체, 설정 프로퍼티
```

의존성 방향: `ui → application → model ← infrastructure`

## 엔티티 관계도

```
Category ──1:N──> Product ──1:N──> Option
                     │
                     └──1:N──> Wish <──N:1── Member

Gift (비영속 객체, JPA 엔티티 아님)
 ├── from: Long (보내는 사람 Member ID)
 ├── to: Long (받는 사람 Member ID)
 ├── option: Option
 ├── quantity: int
 └── message: String
```

## 엔티티 상세

### Category
| 필드 | 타입 | 매핑 |
|------|------|------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | 기본 컬럼 |

### Product
| 필드 | 타입 | 매핑 |
|------|------|------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | 기본 컬럼 |
| price | int | 기본 컬럼 |
| imageUrl | String | 기본 컬럼 |
| category | Category | @ManyToOne (EAGER) |

### Option
| 필드 | 타입 | 매핑 |
|------|------|------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | 기본 컬럼 |
| quantity | int | 기본 컬럼 |
| product | Product | @ManyToOne (EAGER) |

비즈니스 메서드:
- `decrease(int quantity)` — 재고 차감. 재고 부족 시 `IllegalStateException` 발생

### Member
| 필드 | 타입 | 매핑 |
|------|------|------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | 기본 컬럼 |
| email | String | 기본 컬럼 |

### Wish
| 필드 | 타입 | 매핑 |
|------|------|------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| member | Member | @ManyToOne (EAGER) |
| product | Product | @ManyToOne (EAGER) |

## API 엔드포인트

### POST /api/gifts — 선물 보내기

**헤더:** `Member-Id` (보내는 사람 ID)

**요청:**
```json
{
  "optionId": 1,
  "quantity": 2,
  "receiverId": 2,
  "message": "생일 축하해!"
}
```

**응답:** 200 OK (본문 없음)

**처리 흐름:**
1. `GiftRestController.give()` → `GiftService.give()`
2. `optionRepository.findById(optionId)` — 없으면 `NoSuchElementException`
3. `option.decrease(quantity)` — 재고 부족 시 `IllegalStateException`
4. `Gift` 객체 생성 (from, to, option, quantity, message)
5. `giftDelivery.deliver(gift)` — 현재 `FakeGiftDelivery`가 콘솔 출력

전체 과정이 하나의 트랜잭션으로 처리됨. 실패 시 재고 변경 롤백.

### POST /api/products — 상품 등록

**요청:**
```json
{
  "name": "iPhone 15",
  "price": 999,
  "imageUrl": "https://example.com/iphone.jpg",
  "categoryId": 1
}
```

**응답:** 200 OK + Product JSON (category 포함)

**처리 흐름:**
1. `ProductRestController.create()` → `ProductService.create()`
2. `categoryRepository.findById(categoryId)` — 없으면 `NoSuchElementException`
3. `Product` 생성 후 `productRepository.save()`

### GET /api/products — 상품 목록 조회

**응답:** 200 OK + `List<Product>` JSON

### POST /api/categories — 카테고리 등록

**요청:**
```json
{
  "name": "전자제품"
}
```

**응답:** 200 OK + Category JSON

### GET /api/categories — 카테고리 목록 조회

**응답:** 200 OK + `List<Category>` JSON

## 서비스 계층 의존성

| 서비스 | 의존 대상 |
|--------|----------|
| CategoryService | CategoryRepository |
| ProductService | ProductRepository, CategoryRepository |
| OptionService | OptionRepository, ProductRepository |
| WishService | WishRepository, MemberRepository, ProductRepository |
| GiftService | OptionRepository, GiftDelivery |

모든 서비스는 클래스 레벨 `@Transactional` 적용. 생성자 주입 방식.

## Infrastructure 계층

### FakeGiftDelivery
- `GiftDelivery` 인터페이스 구현체 (package-private `@Component`)
- 실제 전송 없이 `System.out.println()`으로 콘솔 출력
- `MemberRepository`를 주입받아 보내는 사람 이름 조회

### KakaoMessageProperties / KakaoSocialProperties
- `@ConfigurationProperties`로 외부 설정 바인딩 (생성자 바인딩, 불변)
- `kakao.message.token`, `kakao.message.url`
- `kakao.social.token`, `kakao.social.url`
- 현재 실제 카카오 API 연동에는 미사용 (FakeGiftDelivery 사용 중)

## 설정 (application.properties)

```properties
spring.application.name=gift
spring.jpa.open-in-view=false          # 트랜잭션 외부 지연 로딩 차단
kakao.message.token=ACCESS_TOKEN
kakao.message.url=https://kapi.kakao.com/v1/api/talk
kakao.social.token=ACCESS_TOKEN
kakao.social.url=https://kapi.kakao.com/v1/api/talk
```

## 예외 처리 현황

| 상황 | 예외 타입 | 발생 위치 |
|------|----------|----------|
| 재고 부족 | `IllegalStateException` | `Option.decrease()` |
| 엔티티 미존재 | `NoSuchElementException` | `Repository.findById().orElseThrow()` |

글로벌 예외 핸들러(`@RestControllerAdvice`)가 없어 위 예외는 500 응답으로 전파됨.

## 현재 미구현 항목

CLAUDE.md 요구 사항 기준으로 아직 구현되지 않은 기능:

| 기능 | 상태 |
|------|------|
| Member 잔액(balance) 필드 및 결제 로직 | 미구현 |
| Gift 상태(status) 필드 및 상태 전이 | 미구현 — Gift는 현재 비영속 객체 |
| 선물 취소 및 재고 복구 | 미구현 |
| 잔액 부족 검증 | 미구현 |
| 인수 테스트 | 미구현 — 테스트 파일 없음 |
| OptionService 컨트롤러 | 미구현 — 서비스만 존재, 엔드포인트 없음 |
| WishService 조회 기능 | 미구현 — create()만 존재 |
