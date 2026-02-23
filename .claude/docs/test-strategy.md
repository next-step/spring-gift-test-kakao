# 인수 테스트 전략 (1단계)

> 이 문서는 1단계(RestAssured 기반) 인수 테스트 설계 기록이다.
> 2단계에서 Cucumber BDD로 전환하면서 형식은 바뀌지만, 시나리오와 검증 기준은 동일하게 유지한다.

시스템 경계(HTTP API)에서 사용자 시나리오 기준으로 테스트하며, 최종 DB 상태를 검증한다.

## 테스트 기술 스택

```
@SpringBootTest(webEnvironment = RANDOM_PORT) + RestAssured
```

- **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** — 실제 서블릿 컨테이너를 랜덤 포트로 기동
- **RestAssured** — 실제 HTTP 요청을 보내는 인수 테스트 클라이언트
- **`@LocalServerPort`** — 기동된 포트를 주입받아 `RestAssured.port`에 설정
- DB 검증이 필요한 경우 `Repository`를 `@Autowired`로 주입하여 직접 조회

### Gradle 의존성

```groovy
testImplementation 'io.rest-assured:rest-assured'
```

Spring Boot BOM이 버전을 관리하므로 버전 명시 불필요.

### 테스트 클래스 기본 구조

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SomeAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}
```

- 각 행위별로 테스트 클래스를 분리한다 (예: `GiftAcceptanceTest`, `CategoryAcceptanceTest`)
- `@BeforeEach`에서 `RestAssured.port` 설정 + DB 초기화

## 테스트 데이터 준비

모든 엔티티를 Repository로 직접 생성한다.

| 엔티티 | 준비 방법 | 이유 |
|--------|----------|------|
| Category | **Repository** (`categoryRepository.save()`) | 테스트 대상이 아닌 API의 버그로 테스트가 실패하는 것을 방지 |
| Product | **Repository** (`productRepository.save()`) | 테스트 대상이 아닌 API의 버그로 테스트가 실패하는 것을 방지 |
| Option | **Repository** (`optionRepository.save()`) | REST 컨트롤러 없음. Repository로 직접 삽입 |
| Member | **Repository** (`memberRepository.save()`) | REST 컨트롤러 없음. Repository로 직접 삽입 |

## 테스트 격리

`@SpringBootTest(RANDOM_PORT)`에서는 테스트와 서버가 별도 스레드에서 실행된다.
테스트 클래스의 `@Transactional`은 서버의 트랜잭션과 무관하므로 **자동 롤백이 불가능**하다.

따라서 `@BeforeEach`에서 명시적으로 DB를 초기화한다.

### DatabaseCleaner

```java
@Component
class DatabaseCleaner {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    void clear() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE wish");
        jdbcTemplate.execute("TRUNCATE TABLE option");
        jdbcTemplate.execute("TRUNCATE TABLE product");
        jdbcTemplate.execute("TRUNCATE TABLE category");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
```

- `SET REFERENTIAL_INTEGRITY FALSE` (H2 전용)로 FK 순서를 무시하고 일괄 TRUNCATE
- `repository.deleteAll()`은 FK 역순을 직접 관리해야 하므로 엔티티 추가 시 깨지기 쉬움

## 외부 의존성 격리 (Stub)

| 컴포넌트 | 실제 vs 대체 | 설명 |
|----------|-------------|------|
| DB | H2 인메모리 | 실제 DB 대신 H2가 대체 |
| 카카오 API | `FakeGiftDelivery` (Stub) | `GiftDelivery` 인터페이스의 유일한 구현체. 콘솔 출력만 수행 |
| Controller → Service → Repository | **실제** | 인수 테스트의 핵심 — 전 구간을 실제로 통과해야 의미 있음 |

## 공통 의사결정

| 의사결정 | 근거 |
|----------|------|
| `@SpringBootTest(RANDOM_PORT)` + RestAssured | 인수 테스트 = 시스템 경계 테스트. 실제 HTTP 요청으로 서블릿 환경을 현실적으로 검증 |
| DB 최종 상태 검증 우선 | 최종 결과를 보호. `verify(mock)` 사용하지 않음 |
| 각 테스트가 자체 데이터 생성 | `data.sql`에 의존하면 테스트 간 결합이 생김 |
| FakeGiftDelivery를 그대로 사용 | 유일한 구현체이며 콘솔 출력만 하므로 테스트에 부작용 없음 |

## 미구현 기능 (테스트 대상 아님)

- 잔액/결제 로직 (Member에 balance 필드 없음)
- 선물 취소 및 상태 전이 (Gift가 JPA 엔티티가 아니며, 상태 필드 없음)
- 선물 상태 조회

---

## 행위별 테스트 시나리오

### 행위 1: 선물하기 (`POST /api/gifts`)

선물 요청을 보내면 재고가 차감되고, 실패 시 시스템 상태가 오염되지 않아야 한다.

**테스트 1-1. 정상 선물 보내기**
- 검증: HTTP 200 + DB Option quantity == 10 - 요청수량

**테스트 1-2. 재고 부족 시 실패**
- Option(quantity=5), 수량 10으로 요청
- 검증: HTTP 500 + quantity == 5 (변경 없음)

**테스트 1-3. 재고 경계값 — 두 번째 선물이 실패**
- Option(quantity=1), 첫 번째 수량1 성공 → 두 번째 수량1 실패
- 검증: 첫 번째 200 + qty==0, 두 번째 500 + qty==0 (음수 방지)

**테스트 1-4. 존재하지 않는 옵션으로 선물 시도**
- 존재하지 않는 optionId로 요청
- 검증: HTTP 500

### 행위 2: 카테고리 등록 (`POST /api/categories`)

**테스트 2-1. 정상 카테고리 등록**
- 검증: HTTP 200 + 응답에 id, name 포함 + DB 존재

### 행위 3: 상품 등록 (`POST /api/products`)

**테스트 3-1. 정상 상품 등록**
- 검증: HTTP 200 + 카테고리 연결 + DB 존재

**테스트 3-2. 존재하지 않는 카테고리로 등록 시도**
- 검증: HTTP 500 + 상품 미생성

### 행위 4: 카테고리 목록 조회 (`GET /api/categories`)

**테스트 4-1. 빈 목록 반환** — 200 + `[]`
**테스트 4-2. 등록 후 조회** — 200 + 2건 포함

### 행위 5: 상품 목록 조회 (`GET /api/products`)

**테스트 5-1. 빈 목록 반환** — 200 + `[]`
**테스트 5-2. 등록 후 조회** — 200 + 2건 + 중첩 카테고리
