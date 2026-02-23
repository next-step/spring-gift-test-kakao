# spring-gift-test

카카오 메시지 API를 활용한 선물하기 서비스 (Spring Boot 기반)

## 기술 스택

- Java 21, Spring Boot 3.5.8, Gradle 8.4
- Spring Data JPA + H2 인메모리 DB
- RestAssured + Cucumber BDD (인수 테스트)

## 주요 기능

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/gifts` | POST | 선물 보내기 (재고 차감 + 카카오 메시지 전송) |
| `/api/products` | GET/POST | 상품 목록 조회 / 등록 |
| `/api/categories` | GET/POST | 카테고리 목록 조회 / 등록 |

## 실행

```bash
./gradlew bootRun          # 애플리케이션 실행
./gradlew test             # Cucumber 인수 테스트 실행
./gradlew restAssuredTest  # RestAssured 인수 테스트 실행
```

## 프로젝트 구조

```
gift/
├── ui/              # REST 컨트롤러
├── application/     # 서비스 + 요청 DTO
├── model/           # JPA 엔티티 + Repository
└── infrastructure/  # 외부 연동 (FakeGiftDelivery, 카카오 API 설정)
```

## 테스트 구조

```
src/test/java/gift/
├── DatabaseCleaner.java          # DB 초기화 (공유)
├── Fixtures.java                 # 테스트 팩토리 메서드 (공유)
├── restassured/                  # RestAssured 기반 인수 테스트
│   ├── AcceptanceTest.java       # 공통 설정 추상 클래스
│   ├── GiftAcceptanceTest.java
│   ├── CategoryAcceptanceTest.java
│   ├── ProductAcceptanceTest.java
│   ├── CategoryRetrieveAcceptanceTest.java
│   └── ProductRetrieveAcceptanceTest.java
└── cucumber/                     # Cucumber BDD 인수 테스트
    ├── CucumberTest.java         # JUnit Suite 러너
    ├── CucumberSpringConfig.java # Spring Boot 통합 설정
    ├── CucumberHooks.java        # @Before: port 설정 + DB 초기화
    ├── ScenarioContext.java      # 시나리오 간 상태 공유
    └── steps/                    # Step Definitions
        ├── GiftStepDefinitions.java
        ├── CategoryStepDefinitions.java
        └── ProductStepDefinitions.java

src/test/resources/features/      # Gherkin Feature 파일
├── gift.feature
├── category.feature
└── product.feature
```

### RestAssured vs Cucumber

동일한 11개 시나리오를 두 가지 방식으로 검증합니다.

- **RestAssured** — 코드 기반. 개발자가 빠르게 읽고 디버깅하기 좋음
- **Cucumber** — Gherkin 기반. 비개발자도 시나리오를 이해할 수 있음

## 테스트 데이터 전략

### Repository + Fixtures 팩토리 방식 채택

```java
// 테스트에서 중요한 값만 노출, 나머지는 기본값
var opt = optionRepository.save(option(10, product));  // 재고 10개
var sender = memberRepository.save(member("보내는사람"));
```

### SQL 파일 방식(`@Sql`, `data.sql`)을 채택하지 않은 이유

| 기준 | Repository + Fixtures | SQL 파일 |
|------|----------------------|----------|
| 컴파일 안전성 | 엔티티 변경 시 컴파일 에러로 즉시 발견 | 런타임에서만 실패 확인 |
| ID 관리 | auto-generated ID를 자연스럽게 사용 | 하드코딩 필요, 테스트 간 충돌 위험 |
| 시나리오별 유연성 | 재고 10개, 5개, 1개 등 테스트마다 자유롭게 변경 | 시나리오마다 별도 SQL 파일 필요 |
| DB 전환 | JPA가 방언 처리 (H2 → PostgreSQL 영향 없음) | SQL 방언 차이로 파일 수정 필요 |
| 가독성 | 팩토리 메서드로 의도가 드러남 | SQL이 장황하고 테스트 코드와 분리됨 |

### 테스트 격리

- `DatabaseCleaner` — 각 테스트 전 `TRUNCATE`로 DB 초기화
- `@Transactional` 롤백에 의존하지 않음 (서버와 테스트가 별도 스레드)

## 테스트 시나리오

### 선물하기 (POST /api/gifts)
- 정상 선물 보내기: 재고 차감 검증
- 재고 부족 시 실패: 롤백으로 재고 불변 검증
- 재고 경계값: 첫 번째 성공 → 두 번째 실패, 음수 방지
- 존재하지 않는 옵션: 에러 응답

### 카테고리 (POST/GET /api/categories)
- 정상 등록: 응답 + DB 검증
- 빈 목록 반환
- 등록 후 목록 조회

### 상품 (POST/GET /api/products)
- 정상 등록: 카테고리 연결 검증
- 존재하지 않는 카테고리로 등록 실패
- 빈 목록 반환
- 등록 후 목록 조회 + 카테고리 중첩 응답

## 과제 진행 과정

### 1단계: RestAssured 기반 인수 테스트
1. CLAUDE.md 작성 — 프로젝트 요구사항 및 개발 제약사항 정리
2. 기능 분석 — 선물하기, 카테고리/상품 등록·조회 흐름 파악
3. 테스트 전략 수립 — 인수 테스트 시나리오 및 데이터 준비 전략 설계
4. 테스트 코드 작성 — `@SpringBootTest` + RestAssured 기반 인수 테스트 구현

### 2단계: Cucumber BDD 전환
1. Cucumber 의존성 추가 및 Spring Boot 통합
2. Gherkin Feature 파일 작성 (한글 Given-When-Then)
3. Step Definitions 구현 (ScenarioContext로 상태 공유)
4. 테스트 패키지 분리 (`restassured/`, `cucumber/`)

## 참고 문서

- [Cucumber BDD 학습 가이드](step2docs/cucumber.md) — Cucumber 설정, Gherkin 문법, Step Definitions 작성법 정리
