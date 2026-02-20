# 프로젝트 설명서

## 개요

선물하기 플랫폼 API 서버. 사용자가 카테고리와 상품을 관리하고, 위시리스트를 구성하며, 다른 회원에게 선물을 보낼 수 있다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.8 |
| 빌드 도구 | Gradle |
| 데이터베이스 | H2 (내장형) |
| 데이터 접근 | Spring Data JPA |
| 템플릿 엔진 | Thymeleaf (의존성 포함, 미사용) |
| 테스트 | JUnit 5 (spring-boot-starter-test) |

## 프로젝트 구조

```
src/main/java/gift/
├── Application.java                     # Spring Boot 진입점
├── ui/                                  # REST 컨트롤러 (프레젠테이션 계층)
│   ├── CategoryRestController.java      # 카테고리 API
│   ├── ProductRestController.java       # 상품 API
│   └── GiftRestController.java          # 선물 API
├── application/                         # 서비스 (비즈니스 로직 계층)
│   ├── CategoryService.java
│   ├── ProductService.java
│   ├── OptionService.java
│   ├── WishService.java
│   ├── GiftService.java
│   ├── CreateCategoryRequest.java       # 요청 DTO
│   ├── CreateProductRequest.java
│   ├── CreateOptionRequest.java
│   ├── CreateWishRequest.java
│   └── GiveGiftRequest.java
├── model/                               # 도메인 모델 및 리포지토리
│   ├── Category.java
│   ├── Product.java
│   ├── Option.java
│   ├── Member.java
│   ├── Wish.java
│   ├── Gift.java                        # 값 객체 (엔티티 아님)
│   ├── GiftDelivery.java               # 전달 인터페이스
│   ├── CategoryRepository.java
│   ├── ProductRepository.java
│   ├── OptionRepository.java
│   ├── MemberRepository.java
│   └── WishRepository.java
└── infrastructure/                      # 외부 연동 (인프라 계층)
    ├── FakeGiftDelivery.java            # GiftDelivery 구현 (콘솔 출력)
    ├── KakaoMessageProperties.java
    └── KakaoSocialProperties.java
```

## 계층 구조

```
Client → ui (Controller) → application (Service) → model (Repository) → H2 DB
                                  │
                                  └──→ model (GiftDelivery) → infrastructure (FakeGiftDelivery)
```

- **ui**: REST 컨트롤러. 요청 수신 및 응답 반환
- **application**: 비즈니스 로직, 트랜잭션 관리. 모든 Service에 `@Transactional` 적용
- **model**: JPA 엔티티, 리포지토리, 도메인 인터페이스
- **infrastructure**: 외부 시스템 연동 구현체

## 도메인 모델

| 엔티티 | 필드 | 관계 |
|--------|------|------|
| Category | id, name | Product(1:N) |
| Product | id, name, price, imageUrl | Category(N:1), Option(1:N) |
| Option | id, name, quantity | Product(N:1) |
| Member | id, name, email | Wish(1:N) |
| Wish | id | Member(N:1), Product(N:1) |
| Gift (값 객체) | from, to, option, quantity, message | - |

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/categories` | 카테고리 생성 |
| GET | `/api/categories` | 카테고리 목록 조회 |
| POST | `/api/products` | 상품 등록 |
| GET | `/api/products` | 상품 목록 조회 |
| POST | `/api/gifts` | 선물 보내기 (헤더: `Member-Id`) |

## 주요 설정

- `spring.jpa.open-in-view=false` — 지연 로딩 범위를 서비스 계층으로 제한
- 카카오톡 API 연동 설정 존재 (`kakao.message.*`, `kakao.social.*`), 현재 미적용

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

## 핵심 비즈니스 로직

선물 보내기(`GiftService.give`)가 핵심 기능이다:
1. 옵션 조회
2. 재고 차감 (`Option.decrease`) — 부족 시 `IllegalStateException`
3. Gift 객체 생성
4. `GiftDelivery` 구현체를 통해 전달

`GiftDelivery` 인터페이스(Strategy 패턴)로 전달 방식을 교체할 수 있다. 현재는 `FakeGiftDelivery`가 콘솔에 출력한다.

## 관련 문서

- [FEATURES.md](FEATURES.md) — 핵심 기능 명세서
- [TEST_STRATEGY.md](TEST_STRATEGY.md) — 테스트 전략 문서
