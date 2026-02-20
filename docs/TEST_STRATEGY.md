# 인수 테스트 작성 계획

## 특이사항 (코드 분석 결과)

1. **상품/카테고리 생성 API가 JSON을 못 받음**
   - `ProductRestController.create()`, `CategoryRestController.create()`에 `@RequestBody`가 없음
   - Spring이 form 바인딩으로 처리하므로, `Content-Type: application/json`으로 보내면 데이터가 바인딩되지 않음
   - 테스트 시 `contentType("application/x-www-form-urlencoded")` + `.formParam()` 사용 필요
2. **미구현 API** — 옵션/위시리스트/회원 관련 컨트롤러 없음 (서비스만 존재)
3. **에러 핸들링 없음** — `NoSuchElementException`, `IllegalStateException`이 잡히지 않아 모두 HTTP 500 반환
4. **Gift는 엔티티가 아님** — Value Object로, DB에 저장되지 않음 (선물 이력 조회 불가)
5. **유효성 검증 전무** — `@NotNull`, `@NotBlank`, `@Min`, `@Valid` 등 Bean Validation 없음. 엔티티에 `unique` 제약조건도 없음. 잘못된 입력이 그대로 저장됨.

---

## 테스트 대상 API

| 메서드 | 경로 | Content-Type | 설명 |
|--------|------|-------------|------|
| POST | `/api/categories` | form | 카테고리 생성 |
| GET | `/api/categories` | - | 카테고리 목록 조회 |
| POST | `/api/products` | form | 상품 등록 |
| GET | `/api/products` | - | 상품 목록 조회 |
| POST | `/api/gifts` | JSON | 선물 전달 (Header: `Member-Id`) |

---

## 검증할 행위 목록

> **테스트 목적**: 잘못된 입력에 대해 200이 나오면 테스트가 실패하도록 만든다.
> 현재 유효성 검증이 없으므로, 아래 실패 시나리오들은 **처음에는 테스트가 실패할 것**이다.
> 이를 통해 프로덕션 코드에 유효성 검증을 추가해야 할 지점을 드러낸다.

### 1. 카테고리 API

| # | 시나리오 | 기대 결과 | 현재 동작 |
|---|---------|----------|----------|
| 1 | 카테고리 정상 생성 | 200, 조회 시 요청한 이름과 동일한 카테고리 존재 | OK |
| 2 | 카테고리 목록 조회 | 200, SQL로 삽입한 카테고리가 목록에 포함 | OK |
| 3 | 이름이 null인 카테고리 생성 | **not 200** (400 계열) | NG — 200으로 저장됨 |
| 4 | 이름이 빈 문자열인 카테고리 생성 | **not 200** (400 계열) | NG — 200으로 저장됨 |
| 5 | 중복된 이름의 카테고리 생성 | **not 200** (409 or 400 계열) | NG — 200으로 저장됨 |

### 2. 상품 API

| # | 시나리오 | 기대 결과 | 현재 동작 |
|---|---------|----------|----------|
| 1 | 상품 정상 생성 (카테고리 존재) | 200, 조회 시 요청한 이름과 동일한 상품 존재 | OK |
| 2 | 상품 목록 조회 | 200, SQL로 삽입한 상품이 목록에 포함 | OK |
| 3 | 존재하지 않는 카테고리로 상품 생성 | **not 200** (500 — `NoSuchElementException`) | OK — 500 반환 |
| 4 | price가 음수인 상품 생성 | **not 200** (400 계열) | NG — 200으로 저장됨 |
| 5 | 이름이 null인 상품 생성 | **not 200** (400 계열) | NG — 200으로 저장됨 |
| 6 | 이름이 빈 문자열인 상품 생성 | **not 200** (400 계열) | NG — 200으로 저장됨 |

### 3. 선물 전달 API

| # | 시나리오 | 기대 결과 | 현재 동작 |
|---|---------|----------|----------|
| 1 | 정상 선물 전달 | 200, 재고가 요청 수량만큼 감소 | OK |
| 2 | 재고 부족으로 실패 | **not 200** (500 — `IllegalStateException`), 재고 변동 없음 | OK |
| 3 | 존재하지 않는 옵션으로 실패 | **not 200** (500 — `NoSuchElementException`) | OK |
| 4 | 수량이 0인 선물 전달 | **not 200** (400 계열) | NG — 200으로 성공, 재고 변동 없음 |
| 5 | 수량이 음수인 선물 전달 | **not 200** (400 계열) | NG — 200으로 성공, 재고가 오히려 증가 |
| 6 | 존재하지 않는 수신자에게 선물 전달 | **not 200** (400 계열) | NG — 200으로 성공 (수신자 존재 검증 없음) |
| 7 | 동시 선물 전달 시 재고 동시성 | 재고가 음수가 되지 않음 | 미확인 |

---

## 테스트 데이터 전략

### 공통
- `@Sql`로 `test-data.sql` 실행 (`executionPhase = BEFORE_TEST_METHOD`)
- 각 테스트 전 데이터 초기화 (H2 인메모리 DB 활용)

### test-data.sql 초기 데이터

```sql
-- 회원 (API 없으므로 직접 삽입)
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');

-- 카테고리
INSERT INTO category (id, name) VALUES (100, '테스트카테고리');

-- 상품
INSERT INTO product (id, name, price, image_url, category_id)
VALUES (100, '테스트상품', 10000, 'http://test.com/image.jpg', 100);

-- 옵션 (API 없으므로 직접 삽입)
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '옵션A', 10, 100);  -- 충분한 재고
INSERT INTO option (id, name, quantity, product_id) VALUES (2, '옵션B', 1, 100);   -- 최소 재고 (실패 테스트용)
```

### 테스트별 데이터 의존관계

| 테스트 | 사전 데이터 |
|--------|-----------|
| 카테고리 생성 (정상/실패) | 없음 (중복 이름 테스트만 SQL로 카테고리 삽입) |
| 카테고리 조회 | SQL로 카테고리 삽입 |
| 상품 생성 (정상) | SQL로 카테고리 삽입 |
| 상품 생성 (실패) | 없음 or SQL로 카테고리 삽입 |
| 상품 조회 | SQL로 카테고리 + 상품 삽입 |
| 선물 전달 (전체) | SQL로 회원 + 카테고리 + 상품 + 옵션 삽입 |

---

## 검증 전략

### 도구
- **RestAssured** 사용

### 검증 방식
1. **응답 코드 검증** — 성공 시 200, 실패 시 not 200 (`.statusCode(not(200))` 또는 구체적인 코드)
2. **생성 후 조회 + 이름 일치 검증** — 생성 API 호출 후 조회 API로 실제 저장 여부와 요청한 이름이 정확히 들어갔는지 확인
3. **재고 변동 검증** — 선물 전달 후 DB 직접 조회로 재고 수량이 정확히 감소했는지 확인
4. **응답 본문 검증** — 조회 API의 경우 반환된 JSON 필드값 검증

### 주의사항
- 상품/카테고리 생성 시 `formParam()`으로 데이터 전송 (`@RequestBody` 없음)
- 선물 전달 시 `Header("Member-Id", memberId)` 필수
- 에러 응답은 모두 500 (별도 에러 핸들러 미구현)
- **NG 표시된 시나리오는 현재 테스트가 실패할 것** — 프로덕션 코드에 유효성 검증 추가 후 통과시켜야 함

---

## 인수테스트 이후 오류 분석

### 1. 데이터 바인딩 실패 (카테고리/상품 생성 API)

- 컨트롤러에 `@RequestBody`가 없어 form 바인딩으로 동작하는데, DTO에 setter도 없음
- 결과: 어떤 값을 보내든 모든 필드가 `null`/`0`으로 저장됨
- **위양성 테스트 존재**: 상품 생성 실패 테스트 4건(음수 가격, null 이름, 빈 이름, 없는 카테고리)이 통과하지만, 유효성 검증이 동작해서가 아니라 `categoryId`가 `null`로 바인딩되어 `NoSuchElementException`이 터지는 것

### 2. 한국어 인코딩 미처리

- 한국어 데이터 전송/응답 시 인코딩이 깨질 수 있음

### 3. 예외 처리 미구현

- `@ControllerAdvice`/`@ExceptionHandler` 없음
- 모든 예외가 Spring 기본 500 응답으로 처리됨
- 구체적으로 누락된 검증:
  - 카테고리: null/빈 이름, 중복 이름
  - 상품: null/빈 이름, 음수 가격
  - 선물: 0/음수 수량, 존재하지 않는 수신자
  - 공통: 동시성 제어 (낙관적/비관적 락 없음)