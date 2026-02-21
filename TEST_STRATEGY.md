# TEST_STRATEGY.md

## 1. 검증할 행위 목록

### 1-1. 선정 기준

1. **상태 변화가 있는 흐름 우선** — 읽기 전용보다 CUD(Create/Update/Delete)가 리팩터링에 취약
2. **리팩터링 위험이 큰 구간 우선** — 트랜잭션 경계, JPA 더티 체킹, 예외 처리가 관여하는 흐름
3. **실패 시 비즈니스 영향이 큰 시나리오 우선** — 재고 정합성 파괴, 데이터 무결성 위반 등

후보 11개(행위리스트2.md §2) 중 아래 7개를 선정했습니다.

### 1-2. 선정된 행위 (7개)

#### Behavior 1: 선물하기 성공 시 옵션 재고가 감소한다

| 항목 | 내용 |
|------|------|
| **사용자 시나리오** | 회원이 충분한 재고가 있는 옵션을 선택하여 다른 회원에게 선물한다 |
| **입력** | `POST /api/gifts` + Header `Member-Id: {senderId}` + Body `{ optionId, quantity: 3, receiverId, message }` |
| **기대 결과** | HTTP 200 OK / 옵션 수량이 10→7로 감소 |
| **실패 시 의미** | 핵심 비즈니스 로직 깨짐. 재고 차감 없이 선물이 발송되거나 차감 수량이 틀어짐 |

#### Behavior 2: 재고 부족 시 선물하기가 거부되고 재고가 유지된다

| 항목 | 내용 |
|------|------|
| **사용자 시나리오** | 옵션 재고보다 많은 수량으로 선물을 시도하면 실패한다 |
| **입력** | `POST /api/gifts` + Body `{ optionId, quantity: 5, ... }` (옵션 재고=2) |
| **기대 결과** | HTTP 500 (IllegalStateException) / 옵션 수량이 2로 유지 (변화 없음) |
| **실패 시 의미** | 마이너스 재고 발생. 트랜잭션 롤백 실패 시 데이터 정합성 파괴 |

#### Behavior 3: 존재하지 않는 옵션으로 선물하면 실패한다

| 항목 | 내용 |
|------|------|
| **사용자 시나리오** | 삭제되었거나 없는 옵션 ID로 선물을 시도하면 실패한다 |
| **입력** | `POST /api/gifts` + Body `{ optionId: 9999, ... }` |
| **기대 결과** | HTTP 500 (NoSuchElementException) |
| **실패 시 의미** | 잘못된 참조로 예상치 못한 에러(NPE 등) 발생 가능 |

#### Behavior 4: 상품을 생성하면 조회 시 반환된다

| 항목 | 내용 |
|------|------|
| **사용자 시나리오** | 카테고리를 지정하여 상품을 생성하고, 이후 목록 조회 시 해당 상품이 포함된다 |
| **입력** | `POST /api/products` + `{ name, price, imageUrl, categoryId }` |
| **기대 결과** | HTTP 200 + 생성된 상품 JSON (id, name, price, imageUrl, category 포함) / `GET /api/products` 시 목록에 포함 |
| **실패 시 의미** | 상품 생성 실패 시 선물하기 전체 흐름이 동작 불가 |

#### Behavior 5: 존재하지 않는 카테고리로 상품 생성 시 실패한다

| 항목 | 내용 |
|------|------|
| **사용자 시나리오** | 없는 카테고리 ID로 상품 생성을 시도하면 실패한다 |
| **입력** | `POST /api/products` + `{ name, price, imageUrl, categoryId: 9999 }` |
| **기대 결과** | HTTP 500 (NoSuchElementException) / Product 미생성 (`GET /api/products` 빈 목록) |
| **실패 시 의미** | 잘못된 카테고리 참조를 가진 상품이 생성되면 데이터 정합성 파괴 |

#### Behavior 6: 카테고리를 생성하면 조회 시 반환된다

| 항목 | 내용 |
|------|------|
| **사용자 시나리오** | 카테고리를 생성하고, 이후 목록 조회 시 해당 카테고리가 포함된다 |
| **입력** | `POST /api/categories` + `{ name }` |
| **기대 결과** | HTTP 200 + 생성된 카테고리 JSON (id, name) / `GET /api/categories` 시 목록에 포함 |
| **실패 시 의미** | 카테고리 생성 실패 시 상품 생성 불가 → 전체 흐름 차단 |

#### Behavior 7: 보내는 회원 미존재 시 선물하기가 실패하고 재고가 롤백된다

| 항목 | 내용 |
|------|------|
| **사용자 시나리오** | 존재하지 않는 회원 ID로 선물을 보내면 실패하고, 이미 차감된 재고가 원복된다 |
| **입력** | `POST /api/gifts` + Header `Member-Id: 9999` + Body `{ optionId, quantity: 3, ... }` (옵션 재고=10) |
| **기대 결과** | HTTP 500 (NoSuchElementException) / 옵션 수량이 10으로 유지 |
| **실패 시 의미** | 코드 순서상 `option.decrease()` → `giftDelivery.deliver()` (회원 조회)이므로, 차감 후 회원 조회 실패 시 롤백이 안 되면 재고만 빠져나감 — 트랜잭션 원자성 위반 |

---

## 2. 테스트 데이터 전략

### 2-1. 데이터 준비 (Arrange) — `@Sql` + SQL 파일

초기 데이터를 Java 코드(Repository/Service)가 아닌 **SQL 파일**로 관리하고, `@Sql` 애노테이션으로 테스트 실행 전에 로드합니다.

| 항목 | 내용 |
|------|------|
| **위치** | `src/test/resources/sql/` 디렉토리 |
| **적용 방식** | 테스트 클래스 또는 메서드에 `@Sql` 애노테이션 선언 |
| **실행 시점** | `executionPhase = BEFORE_TEST_METHOD` (기본값) |

#### SQL 파일 구성

| SQL 파일 | 용도 | 사용하는 Behavior |
|----------|------|-------------------|
| `cleanup.sql` | 모든 테이블 TRUNCATE (테스트 간 격리) | 전체 |
| `gift-setup.sql` | 선물하기 테스트에 필요한 데이터 (회원, 카테고리, 상품, 옵션) | B1, B2, B7 |
| `gift-setup-low-stock.sql` | 재고 부족 시나리오용 (옵션 수량=2) | B2 |
| `product-setup.sql` | 상품 생성 테스트에 필요한 데이터 (카테고리) | B4, B5 |

#### `@Sql` 사용 패턴

```java
// 클래스 레벨: 매 테스트 전 cleanup 실행
@Sql(scripts = "/sql/cleanup.sql", executionPhase = BEFORE_TEST_METHOD)

// 메서드 레벨: cleanup 후 시나리오별 데이터 로드
@Sql({"/sql/cleanup.sql", "/sql/gift-setup.sql"})
void should_decrease_option_quantity_when_gift_is_sent_successfully() { ... }
```

#### 왜 SQL 파일인가

| 관점 | Repository 직접 호출 | `@Sql` SQL 파일 |
|------|---------------------|-----------------|
| 가독성 | Java 코드에 Arrange 로직 혼재 | SQL 파일로 분리되어 테스트 코드가 When/Then에 집중 |
| 재사용 | 테스트마다 중복 코드 | SQL 파일 공유로 중복 제거 |
| 투명성 | 엔티티 생성자/메서드에 의존 | 테이블/컬럼 기준으로 데이터가 명확히 보임 |
| 리팩터링 내성 | 엔티티 생성자 변경 시 테스트도 수정 필요 | 테이블 스키마가 유지되면 SQL 불변 |

### 2-2. 데이터 격리 (Clean-up) — TRUNCATE via `@Sql`

| 방식 | 설명 |
|------|------|
| **`@Sql` TRUNCATE** | 매 테스트 실행 전 `cleanup.sql`로 전체 테이블을 비움 |
| **FK 순서** | TRUNCATE 시 FK 제약조건에 의한 실패를 방지하기 위해 `SET REFERENTIAL_INTEGRITY FALSE` → TRUNCATE → `SET REFERENTIAL_INTEGRITY TRUE` (H2 문법) |
| **`@Transactional` 미사용** | 테스트에 `@Transactional`을 붙이지 않음. 서비스의 트랜잭션 경계를 실제와 동일하게 검증하기 위함 |

`cleanup.sql` 예시:
```sql
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE wish;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;
```

### 2-3. 결정: 테스트 인프라 선택

| 구분 | 선택 | 이유 |
|------|------|------|
| **전체 Behavior** | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` | 실제 HTTP 경로 + 실제 트랜잭션 커밋/롤백 검증 |
| **데이터 준비** | `@Sql` + SQL 파일 (`src/test/resources/sql/`) | 테스트 코드와 데이터 분리, 재사용성, 리팩터링 내성 |
| **데이터 격리** | `@Sql`로 매 테스트 전 TRUNCATE | `@Transactional` 롤백 대신 명시적 정리로 실제 커밋 동작을 보장 |

---

## 3. 검증 전략

### 3-1. 무엇을 검증하는가 (우선순위)

CLAUDE.md "What to assert (priority order)"를 따릅니다.

| 우선순위 | 검증 대상 | 구체적 방법 |
|----------|-----------|-------------|
| 1순위 | **HTTP 응답** (상태코드 + 바디) | `TestRestTemplate`으로 API 호출 후 `ResponseEntity`의 statusCode, body 확인 |
| 2순위 | **후속 행동으로 검증** | 생성 후 조회 API(`GET`)를 호출하여 결과 확인 — 조회 API가 있는 경우 DB 직접 조회보다 우선 |
| 3순위 | **DB 상태 변화** (최소 범위) | 조회 API가 없는 경우에 한해 `Repository`로 직접 확인 (Option quantity 등) |

### 3-2. 행위별 검증 방법

| Behavior | 응답 검증 | 후속 행동 검증 | DB 직접 검증 |
|----------|-----------|----------------|--------------|
| **1** 선물 성공 → 재고 감소 | HTTP 200 | — | `OptionRepository.findById()` → quantity 확인 (조회 API 미노출) |
| **2** 재고 부족 → 거부 | HTTP 500 | — | `OptionRepository.findById()` → quantity 불변 확인 |
| **3** 없는 옵션 → 실패 | HTTP 500 | — | — |
| **4** 상품 생성 → 조회 | HTTP 200 + 응답 body 필드 확인 | `GET /api/products` → 목록에 포함 | — |
| **5** 없는 카테고리 → 실패 | HTTP 500 | `GET /api/products` → 빈 목록 | — |
| **6** 카테고리 생성 → 조회 | HTTP 200 + 응답 body 필드 확인 | `GET /api/categories` → 목록에 포함 | — |
| **7** 회원 미존재 → 실패 + 롤백 | HTTP 500 | — | `OptionRepository.findById()` → quantity 불변 확인 |

### 3-3. 검증하지 않는 것

CLAUDE.md "What NOT to assert"를 따릅니다.

- `verify(option).decrease(3)` 같은 내부 메서드 호출 여부
- 특정 클래스의 존재/구조
- Mock verify 기반의 내부 구현 계약
- `FakeGiftDelivery`의 `System.out.println` 호출 여부

---

## 4. 주요 의사결정

### Decision 1: 데이터 준비 방식 — Repository 호출 vs `@Sql` SQL 파일

| 선택지 | 장점 | 단점 |
|--------|------|------|
| Repository/Service 호출 | 컴파일 타임 검증, IDE 자동완성 | 엔티티 생성자 변경 시 테스트도 수정 필요. Arrange 코드가 테스트에 혼재 |
| **`@Sql` SQL 파일 (선택)** | 테스트 코드와 데이터 분리. 테이블 스키마 기준이므로 리팩터링 내성 높음 | SQL 작성 필요. 엔티티-테이블 매핑을 알아야 함 |

**결정: `@Sql` SQL 파일**
- 테스트 코드가 When/Then에 집중할 수 있음 (Given이 SQL로 분리)
- 여러 테스트가 동일 시나리오 데이터를 재사용 가능
- 엔티티 생성자/내부 구조가 바뀌어도 테이블 스키마만 유지되면 SQL 불변 → 리팩터링 안전망 역할에 적합

### Decision 2: 데이터 격리 — `@Transactional` 롤백 vs `@Sql` TRUNCATE

| 선택지 | 장점 | 단점 |
|--------|------|------|
| `@Transactional` 롤백 | 간단, 빠름, 격리 확실 | 서비스의 트랜잭션 경계를 테스트가 덮어씀. 롤백 검증 불가 |
| **`@Sql` TRUNCATE (선택)** | 실제 트랜잭션 커밋/롤백 동작 검증 가능 | 매 테스트 전 TRUNCATE 실행 비용 |

**결정: `@Sql` TRUNCATE**
- Behavior 2, 7에서 "실패 시 트랜잭션 롤백으로 재고가 유지되는가"를 검증하는 것이 핵심
- `@Transactional` 테스트에서는 테스트 자체의 트랜잭션이 서비스 트랜잭션을 감싸버려서 롤백 여부를 정확히 확인할 수 없음
- `RANDOM_PORT` + `TestRestTemplate` + `@Sql` TRUNCATE 조합으로 실제 HTTP 요청 후 별도 트랜잭션에서 DB 상태를 확인

### Decision 3: MockMvc vs TestRestTemplate

| 선택지 | 장점 | 단점 |
|--------|------|------|
| MockMvc | 빠름, 서블릿 컨테이너 불필요 | 실제 HTTP 요청이 아님. `@Transactional`과 같은 문제 |
| **TestRestTemplate (선택)** | 실제 서블릿 컨테이너 + HTTP 요청. 실제 운영과 동일한 경로 | 약간 느림 |

**결정: TestRestTemplate**
- 행동 기반 테스트의 목적은 "사용자 관점의 외부 행동"을 보호하는 것
- 실제 HTTP 요청 → 서블릿 → 컨트롤러 → 서비스 → DB 전체 경로를 거치는 것이 외부 행동 보호에 적합
- 트랜잭션 경계도 실제와 동일하게 동작

### Decision 4: Option 재고 검증 — 조회 API vs Repository 직접 조회

| 선택지 | 장점 | 단점 |
|--------|------|------|
| 조회 API로 검증 | 외부 행동만으로 검증, 리팩터링 내성 높음 | Option 조회 API가 없음 (Controller 미노출) |
| **Repository 직접 조회 (선택)** | 현재 유일한 확인 수단 | 내부 구현에 약간 의존 |

**결정: Repository 직접 조회 (최소 범위)**
- CLAUDE.md: "레거시 상황에서 읽기 전용 검증이 불가피하면 최소 범위로 DB를 조회할 수 있음"
- Option 조회 API가 없으므로 `OptionRepository.findById()`로 quantity만 확인
- 추후 Option 조회 API가 추가되면 해당 API로 전환

### Decision 5: 카테고리/상품 생성 API의 바인딩 방식

`CategoryRestController`와 `ProductRestController`의 POST 메서드에는 `@RequestBody`가 없습니다.
Spring MVC에서 이 경우 form/query 파라미터로 바인딩됩니다.

| 선택지 | 설명 |
|--------|------|
| JSON body로 전송 | `@RequestBody` 없으므로 바인딩 실패 |
| **form/query 파라미터로 전송 (선택)** | 현재 코드의 실제 동작과 일치 |

**결정: form 파라미터로 전송**
- `POST /api/categories`: `application/x-www-form-urlencoded` 또는 query string으로 `name` 전달
- `POST /api/products`: 동일 방식으로 `name`, `price`, `imageUrl`, `categoryId` 전달
- `POST /api/gifts`만 `@RequestBody` JSON으로 전송 (코드에 명시됨)

### Decision 6: 테스트 파일 구성

| 선택지 | 설명 |
|--------|------|
| 행위 1개 = 파일 1개 | 파일이 많아짐 |
| **도메인 단위로 그룹핑 (선택)** | 관련 행위를 묶어 컨텍스트 공유 |

**결정: 도메인 단위 그룹핑**

| 테스트 파일 | 포함 Behavior |
|-------------|---------------|
| `GiftBehaviorTest` | B1 (선물 성공+재고감소), B2 (재고부족), B3 (없는 옵션), B7 (회원 미존재+롤백) |
| `ProductBehaviorTest` | B4 (상품 생성+조회), B5 (없는 카테고리) |
| `CategoryBehaviorTest` | B6 (카테고리 생성+조회) |

---

## 5. 리팩터링 위험 지점 (테스트로 보호할 대상)

| 위험 지점 | 위치 | 보호하는 Behavior |
|-----------|------|-------------------|
| 트랜잭션 내 재고 차감 + 외부 호출 | `GiftService.give()` | B1, B2, B7 |
| JPA 더티 체킹 의존 (명시적 save 없음) | `GiftService.give()` | B1 |
| 트랜잭션 롤백 보장 | `GiftService.give()` | B2, B7 |
| `orElseThrow()` 예외 계약 | `GiftService`, `ProductService` | B3, B5 |
| Category-Product FK 연관 | `ProductService.create()` | B4, B5 |
| 응답 직렬화 (JPA 연관 엔티티) | `ProductRestController` | B4 |

---

## 6. 실행 명령

```bash
./gradlew test
```

- 테스트 프레임워크: JUnit 5 (`useJUnitPlatform()`)
- DB: H2 인메모리 (별도 설정 불필요)
- 테스트 의존성: `spring-boot-starter-test` (JUnit 5 + AssertJ + Mockito + TestRestTemplate 포함)
