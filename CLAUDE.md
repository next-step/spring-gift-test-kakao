# CLAUDE.md — Spring Gift Test (Kakao)

## 프로젝트 개요

카카오 연동 선물 관리/배송 시스템. 사용자가 상품, 카테고리, 옵션, 위시리스트를 관리하고 선물을 전달할 수 있다.

## 기술 스택

- Java 21, Spring Boot 3.5.8, Spring Data JPA, Thymeleaf
- DB: PostgreSQL (Docker Compose), H2 (로컬 개발 fallback)
- Build: Gradle
- Test: Cucumber 7 + JUnit 5 Platform, Spring Boot Test
- BDD: Gherkin (.feature 파일) + Step Definitions
- Infra: Docker, Docker Compose (PostgreSQL + Application 컨테이너)

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
./gradlew bootRun           # 애플리케이션 실행
./gradlew test              # 전체 테스트 실행
./gradlew cucumberTest      # Cucumber 인수 테스트 실행 (PostgreSQL 자동 시작)

# Docker 기반 E2E
./gradlew dockerBuild       # 애플리케이션 Docker 이미지 빌드
./gradlew dockerUp          # Docker Compose로 전체 시스템 시작
./gradlew dockerDown        # Docker Compose 종료
```

## 테스트 전략 (필수 준수사항)

### 원칙

1. **BDD(Cucumber + Gherkin)로 인수 테스트를 작성한다.**
   - Gherkin `.feature` 파일로 비즈니스 시나리오를 기술하고, Step Definitions로 자동화한다.
   - 기술 용어(HTTP, JSON, API) 대신 **도메인 언어(비즈니스 용어)**로 표현한다.
   - 구현이 바뀌어도 Gherkin 시나리오는 유지되어야 한다.

2. **테스트 인프라 패턴을 준수한다.**
   - **ScenarioContext**: Step 간 상태 공유는 `@ScenarioScope` 빈으로 관리한다.
   - **Step Definitions**: Gherkin 문장과 Java 메서드를 연결하는 Adapter 역할. 재사용 가능하되 명확하게 작성한다.
   - **Test Doubles**: 외부 의존(`GiftDelivery`)은 Fake/Stub으로 격리한다.
   - **DB 격리**: 각 시나리오마다 DB를 초기화하여 테스트 간 데이터가 간섭하지 않도록 한다.

3. **테스트 환경은 Docker Compose로 구성한다.**
   - PostgreSQL은 Docker Compose로 실행하고, Spring 프로파일로 테스트/개발 DB를 분리한다.
   - 애플리케이션도 Docker 컨테이너로 실행하여 프로덕션과 동일한 환경에서 E2E 테스트를 수행한다.
   - Dockerfile은 Multi-stage build로 작성한다.

4. **테스트 전략 문서를 반드시 따른다.**
   - 테스트 코드를 작성하기 전에 `TEST_STRATEGY.md`를 반드시 읽고 확인한다.
   - 테스트 전략 문서에 정의된 규칙과 패턴을 준수한다.

5. **TEST_ANALYSIS.md의 행위 목록과 Gherkin 시나리오를 기준으로 작성한다.**
   - 5개 행위(카테고리 생성/조회, 상품 생성/조회, 선물 전달)에 대한 시나리오를 구현한다.
   - Feature 파일은 `category.feature`, `product.feature`, `gift.feature`로 나눈다.
   - 리스크 기반으로 자동화 대상을 선정한다 (High Risk → 자동화, Low Risk → 수동 QA).