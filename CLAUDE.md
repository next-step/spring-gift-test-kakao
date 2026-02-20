# CLAUDE.md — Spring Gift Test (Kakao)

## 프로젝트 개요

카카오 연동 선물 관리/배송 시스템. 사용자가 상품, 카테고리, 옵션, 위시리스트를 관리하고 선물을 전달할 수 있다.

## 기술 스택

- Java 21, Spring Boot 3.5.8, Spring Data JPA, H2 (In-Memory), Thymeleaf
- Build: Gradle
- Test: JUnit 5 (spring-boot-starter-test)

## 프로젝트 구조

```
src/main/java/gift/
├── model/              # 도메인 (Entity, Repository)
├── application/        # 서비스 (Service, Request DTO)
├── ui/                 # REST Controller
└── infrastructure/     # 외부 서비스, 설정 (Kakao 연동 등)
```

### 주요 도메인

- **Category** — 상품 카테고리 (1:N → Product)
- **Product** — 상품 (N:1 → Category, 1:N → Option, 1:N → Wish)
- **Option** — 상품 옵션/재고 관리 (`decrease()`로 재고 차감, 부족 시 IllegalStateException)
- **Member** — 사용자 (1:N → Wish)
- **Wish** — 위시리스트 항목 (N:1 → Member, N:1 → Product)
- **Gift** — 선물 전달 객체 (Entity 아님, Transfer Object)

### API 엔드포인트

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/categories` | 카테고리 생성 |
| GET | `/api/categories` | 전체 카테고리 조회 |
| POST | `/api/products` | 상품 생성 |
| GET | `/api/products` | 전체 상품 조회 |
| POST | `/api/gifts` | 선물 전달 (Header: `Member-Id`) |

### 설계 패턴

- **레이어드 아키텍처** — model → application → ui / infrastructure
- **Strategy Pattern** — `GiftDelivery` 인터페이스, 현재 `FakeGiftDelivery`가 콘솔 출력으로 동작
- **DTO Pattern** — Request 객체 분리 (`CreateXxxRequest`, `GiveGiftRequest`)

### 설정

- `spring.jpa.open-in-view=false` (트랜잭션 외부 Lazy Loading 방지)
- 카카오 API 설정: `kakao.message.*`, `kakao.social.*` (현재 플레이스홀더)

## 빌드 및 실행

```bash
./gradlew bootRun        # 애플리케이션 실행
./gradlew test           # 테스트 실행
```

## 테스트 전략 (필수 준수사항)

### 원칙

1. **인수 테스트(Acceptance Test)만 작성한다.**
   - 단위 테스트가 아닌, API 레벨의 인수 테스트를 작성한다.
   - 리팩토링을 하더라도 테스트 코드를 수정할 필요가 없어야 한다.
   - 내부 구현이 아닌 외부 동작(HTTP 요청/응답)을 검증한다.

2. **BDD 도구를 사용하지 않는다.**
   - Cucumber, Karate 등의 BDD 프레임워크를 사용하지 않는다.
   - JUnit 5 + Spring Boot Test (`@SpringBootTest`, `TestRestTemplate` 또는 `MockMvc`)를 사용한다.

3. **테스트 전략 문서를 반드시 따른다.**
   - 테스트 코드를 작성하기 전에 `TEST_STRATEGY.md`를 반드시 읽고 확인한다.
   - 테스트 전략 문서에 정의된 규칙과 패턴을 준수한다.