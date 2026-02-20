# 인수 테스트 전략

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

API 우선, Repository 보조 원칙을 따른다.

| 엔티티 | 준비 방법 | 이유 |
|--------|----------|------|
| Category | **API** (`POST /api/categories`) | REST 컨트롤러 존재. 시스템 경계를 통과하여 생성 |
| Product | **API** (`POST /api/products`) | REST 컨트롤러 존재. 폼 바인딩 → 서비스 → DB 전 구간 검증 |
| Option | **Repository** (`optionRepository.save()`) | REST 컨트롤러 없음. Repository로 직접 삽입 |
| Member | **Repository** (`memberRepository.save()`) | REST 컨트롤러 없음. Repository로 직접 삽입 |

- API가 있는 엔티티는 API로 생성하여 인수 테스트 철학(시스템 경계 사용)에 충실한다
- API가 없는 엔티티만 Repository로 보조한다. 이것은 Mock이 아니라 단순한 데이터 삽입이다

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
- 각 테스트 클래스에서 `@Autowired DatabaseCleaner` + `@BeforeEach`에서 `cleaner.clear()` 호출

## 외부 의존성 격리 (Stub)

| 컴포넌트 | 실제 vs 대체 | 설명 |
|----------|-------------|------|
| DB | H2 인메모리 | 실제 DB 대신 H2가 대체 |
| 카카오 API | `FakeGiftDelivery` (Stub) | `GiftDelivery` 인터페이스의 유일한 구현체. 콘솔 출력만 수행 |
| Controller → Service → Repository | **실제** | 인수 테스트의 핵심 — 전 구간을 실제로 통과해야 의미 있음 |

- `FakeGiftDelivery`는 포트/어댑터 패턴의 Stub으로, 외부 시스템(카카오 API) 경계를 대체한다
- 내부 컴포넌트에 `@MockBean`을 사용하지 않는다. Mock하면 해당 구간이 검증에서 빠진다

## 공통 의사결정

| 의사결정 | 근거 |
|----------|------|
| `@SpringBootTest(RANDOM_PORT)` + RestAssured | 인수 테스트 = 시스템 경계 테스트. 실제 HTTP 요청으로 서블릿 환경을 현실적으로 검증 |
| DB 최종 상태 검증 우선 | CLAUDE.md 원칙: 최종 결과를 보호. `verify(mock)` 사용하지 않음 |
| 각 테스트가 자체 데이터 생성 | `data.sql`에 의존하면 테스트 간 결합이 생김. 각 테스트가 독립적으로 데이터 준비 |
| FakeGiftDelivery를 그대로 사용 | 유일한 구현체이며 콘솔 출력만 하므로 테스트에 부작용 없음. 별도 Mock 불필요 |
| BDD 도구 미사용 | 제약 조건에 명시. JUnit 5 + Spring Boot Test + RestAssured만 사용 |
| findAll() 테스트 시 DB 초기화 후 정확한 검증 | `@BeforeEach`에서 테이블 truncate → 테스트가 데이터 생성 → 정확한 건수/내용 단언 가능 |

## 미구현 기능 (테스트 대상 아님)

CLAUDE.md 요구사항에 기술되어 있으나 현재 코드에 구현되어 있지 않으므로 테스트 대상에서 제외한다.

- 잔액/결제 로직 (Member에 balance 필드 없음)
- 선물 취소 및 상태 전이 (Gift가 JPA 엔티티가 아니며, 상태 필드 없음)
- 선물 상태 조회

---

## 행위 1: 선물하기 (`POST /api/gifts`)

선물 요청을 보내면 재고가 차감되고, 실패 시 시스템 상태가 오염되지 않아야 한다.

### 테스트 1-1. 정상 선물 보내기

**선정 이유:** 가장 기본적인 성공 경로. 이것이 깨지면 나머지는 의미가 없다.

**테스트 데이터:**
- Category, Product, Option(quantity=10), Member 2명을 생성

**검증:**
- HTTP `200 OK`
- DB Option 재조회 → `quantity == 10 - 요청수량`

**의사결정:**
- `giftDelivery.deliver()` 호출 여부는 검증하지 않음 (최종 DB 상태가 보호 대상)
- 현재 엔드포인트가 `void`를 반환하므로 상태 코드만 검증

### 테스트 1-2. 재고 부족 시 실패

**선정 이유:** `Option.decrease()`의 가드 로직이 API 경계까지 전파되는지, 실패 시 트랜잭션 롤백이 정상 작동하는지 확인한다.

**테스트 데이터:**
- Option(quantity=5) 생성, 수량 10으로 선물 요청

**검증:**
- HTTP `500`
- DB Option 재조회 → `quantity == 5` (변경 없음)

**의사결정:**
- 단순히 에러 반환만으로는 불충분. 롤백 실패 시 선물 없이 재고만 줄어드는 상황이 가능하므로 **DB 최종 상태의 불변성 검증이 핵심**.

### 테스트 1-3. 재고 경계값 — 두 번째 선물이 실패

**선정 이유:** 가장 현실적인 시나리오. 재고 경계값에서 트랜잭션 커밋/롤백이 정확히 작동하는지 확인한다.

**테스트 데이터:**
- Option(quantity=**1**) 생성
- 첫 번째 요청: 수량 1 (성공 기대)
- 두 번째 요청: 수량 1 (실패 기대)

**검증:**
- 첫 번째: `200 OK` + DB quantity == 0
- 두 번째: `500` + DB quantity == 0 (음수로 내려가지 않음)

**의사결정:**
- **두 요청을 하나의 테스트에서 실행** — 상태 연속성 시나리오이므로 분리하면 의미가 없다
- **quantity == 0을 두 번 검증** — 첫 성공 후 0인지, 두 번째 실패 후에도 여전히 0인지 확인

### 테스트 1-4. 존재하지 않는 옵션으로 선물 시도

**선정 이유:** `findById().orElseThrow()` 경로 테스트. 잘못된 입력이 시스템을 오염시키지 않는지 확인한다.

**테스트 데이터:**
- 존재하지 않는 optionId(예: 999999)로 요청

**검증:**
- HTTP `500`
- (선택) 다른 Option 데이터가 있다면 해당 quantity가 변경되지 않았는지 확인

**의사결정:**
- 이 테스트 없이는 `NoSuchElementException`이 API 경계에서 예상대로 동작하는지 보장할 수 없다

---

## 행위 2: 카테고리 등록 (`POST /api/categories`)

카테고리를 등록하면 DB에 저장되고, 목록 조회 시 포함되어야 한다.

### 테스트 2-1. 정상 카테고리 등록

**선정 이유:** 기본 성공 경로. 등록 API의 요청-응답-DB 저장 흐름이 정상 작동하는지 확인한다.

**테스트 데이터:**
- `name: "식품"`으로 POST 요청

**검증:**
- HTTP `200 OK`
- 응답 Body에 `id`(자동 생성)와 `name: "식품"` 포함
- DB에서 Category 조회 → 해당 name의 행이 존재

**의사결정:**
- 현재 `@RequestBody` 누락으로 폼 바인딩(`application/x-www-form-urlencoded`)으로 동작함. 테스트도 이 동작에 맞춰 요청해야 한다.
- 엔티티를 직접 반환하므로 응답 구조가 `{ "id": ..., "name": ... }` 형태

### 테스트 2-2. name 없이 등록 시도

**선정 이유:** 현재 입력 검증이 없으므로 name 없이 요청했을 때 시스템이 어떻게 반응하는지 확인한다.

**테스트 데이터:**
- name 파라미터 없이 POST 요청

**검증:**
- HTTP 상태 코드 확인 (현재 검증 없이 DB까지 도달하므로 `500` 또는 DB 제약 조건에 따라 다름)
- DB에 name이 null인 카테고리가 생성되지 않아야 함

**의사결정:**
- 현재 동작을 기록하는 성격의 테스트. 향후 Bean Validation 추가 시 기대 응답이 `400`으로 변경될 수 있다.
```
<현재는 없지만 향후 추가될 수 있는 입력 검증에 대한 테스트 시나리오>
### 테스트 2-4. 같은 이름으로 중복 등록 불가

**선정 이유:** 카테고리 이름은 고유해야 한다. 중복 등록 시 시스템이 이를 거부하는지 확인한다.

**테스트 데이터:**
- `name: "식품"`으로 첫 번째 등록 (성공 기대)
- `name: "식품"`으로 두 번째 등록 (실패 기대)

**검증:**
- 첫 번째: `200 OK` + DB에 카테고리 저장됨
- 두 번째: 에러 응답 + DB에 같은 이름의 카테고리가 1건만 존재

**의사결정:**
- 현재 코드에 unique 제약 조건이 없으므로, 이 테스트는 **아직 구현되지 않은 제약을 드러내는 역할**을 한다. 중복 방지 로직(DB unique 제약 또는 서비스 레벨 검증) 추가 시 이 테스트가 통과해야 한다.
```

---

## 행위 3: 상품 등록 (`POST /api/products`)

상품을 등록하면 카테고리와 연결되어 DB에 저장되고, 목록 조회 시 포함되어야 한다.

### 테스트 3-1. 정상 상품 등록

**선정 이유:** 기본 성공 경로. 카테고리 조회 → 상품 저장 흐름이 정상 작동하는지 확인한다.

**테스트 데이터:**
- Category를 먼저 등록
- `name: "아이폰 16"`, `price: 1500000`, `imageUrl: "https://..."`, `categoryId: {등록된 카테고리 ID}`로 POST 요청

**검증:**
- HTTP `200 OK`
- 응답 Body에 `id`, `name`, `price`, `imageUrl`, `category` 포함
- DB에서 Product 조회 → 해당 상품이 올바른 카테고리와 연결되어 존재

**의사결정:**
- 카테고리가 사전 조건이므로 테스트 내에서 카테고리를 먼저 생성해야 한다
- `@RequestBody` 누락으로 폼 바인딩 동작. 테스트도 이에 맞춰 요청

### 테스트 3-2. 존재하지 않는 카테고리로 등록 시도

**선정 이유:** `categoryRepository.findById().orElseThrow()` 경로 테스트. 잘못된 categoryId가 시스템을 오염시키지 않는지 확인한다.

**테스트 데이터:**
- 존재하지 않는 categoryId(예: 999999)로 상품 등록 요청

**검증:**
- HTTP `500`
- DB에 상품이 생성되지 않아야 함

**의사결정:**
- 카테고리 의존 관계가 상품 등록의 핵심 제약이므로, 이 경로를 반드시 검증해야 한다

---

## 행위 4: 카테고리 목록 조회 (`GET /api/categories`)

카테고리 목록 조회 시 등록된 데이터를 정확히 반환해야 한다.

### 테스트 4-1. 데이터가 없을 때 빈 목록 반환

**선정 이유:** 가장 기본적인 경로. 데이터 없이도 에러 없이 응답하는지 확인한다.

**테스트 데이터:**
- DB 초기화 후 GET 요청

**검증:**
- HTTP `200 OK`
- 응답 Body가 빈 배열 `[]`

**의사결정:**
- DB truncate 후 실행하므로 다른 테스트의 데이터가 간섭하지 않음

### 테스트 4-2. 등록한 카테고리가 목록에 포함

**선정 이유:** 등록과 조회가 연결되는 시나리오. 등록한 데이터가 실제로 조회 API에 반영되는지 확인한다.

**테스트 데이터:**
- 카테고리 2개 등록 (`"식품"`, `"전자기기"`)
- 이후 `GET /api/categories` 조회

**검증:**
- HTTP `200 OK`
- GET 응답에 등록한 2개 카테고리가 모두 포함
- 각 항목의 `id`와 `name`이 등록 시 반환된 값과 일치
- DB 초기화 후 실행하므로 정확히 2건인지도 검증 가능

**의사결정:**
- **등록과 조회를 하나의 테스트에서 실행** — 등록 결과가 조회에 반영되는지가 핵심이므로 분리하면 의미가 없다

---

## 행위 5: 상품 목록 조회 (`GET /api/products`)

상품 목록 조회 시 등록된 데이터와 연관된 카테고리 정보를 정확히 반환해야 한다.

### 테스트 5-1. 데이터가 없을 때 빈 목록 반환

**선정 이유:** 가장 기본적인 경로. 데이터 없이도 에러 없이 응답하는지 확인한다.

**테스트 데이터:**
- DB 초기화 후 GET 요청

**검증:**
- HTTP `200 OK`
- 응답 Body가 빈 배열 `[]`

**의사결정:**
- DB truncate 후 실행하므로 다른 테스트의 데이터가 간섭하지 않음

### 테스트 5-2. 등록한 상품이 목록에 포함 + 카테고리 중첩 응답

**선정 이유:** 등록한 상품이 조회 API에 반영되는지, 카테고리 정보가 함께 포함되는지 확인한다.

**테스트 데이터:**
- 카테고리 등록 후 상품 2개 등록
- `GET /api/products` 조회

**검증:**
- HTTP `200 OK`
- GET 응답에 등록한 2개 상품이 모두 포함
- 각 상품의 `category` 필드에 올바른 카테고리 정보가 중첩되어 있음
- DB 초기화 후 실행하므로 정확히 2건인지도 검증 가능

**의사결정:**
- `@ManyToOne` 관계가 응답에 정상 직렬화되는지가 핵심. `open-in-view=false` 설정이므로 트랜잭션 내에서 연관 엔티티가 로딩되어야 한다
- **등록과 조회를 하나의 테스트에서 실행** — 등록 결과가 조회에 반영되는지가 핵심이므로 분리하면 의미가 없다
