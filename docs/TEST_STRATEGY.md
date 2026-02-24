# 인수 테스트 작성 계획

## 특이사항 (코드 분석 결과)

1. **미구현 API** — 옵션/위시리스트/회원 관련 컨트롤러 없음 (서비스만 존재)
2. **에러 핸들링 없음** — `NoSuchElementException`, `IllegalStateException`이 잡히지 않아 모두 HTTP 500 반환
3. **Gift는 엔티티가 아님** — Value Object로, DB에 저장되지 않음 (선물 이력 조회 불가)
4. **유효성 검증 전무** — `@NotNull`, `@NotBlank`, `@Min`, `@Valid` 등 Bean Validation 없음. 엔티티에 `unique` 제약조건도 없음. 잘못된 입력이 그대로 저장됨.

---

## 테스트 대상 API

| 메서드 | 경로 | Content-Type | 설명 |
|--------|------|-------------|---
---|
| POST | `/api/categories` | JSON | 카테고리 생성 |
| GET | `/api/categories` | - | 카테고리 목록 조회 |
| POST | `/api/products` | JSON | 상품 등록 |
| GET | `/api/products` | - | 상품 목록 조회 |
| POST | `/api/gifts` | JSON | 선물 전달 (Header: `Member-Id`) |

---

## 검증할 행위 목록

> **테스트 원칙**: 프로덕션 코드에 실제로 존재하는 행위만 검증한다.
> 프로덕션에 없는 검증 로직(입력값 유효성 검사 등)에 대한 테스트는 작성하지 않는다.

### 1. 카테고리 API

| # | 시나리오 | 기대 결과 | 근거 |
|---|---------|----------|------|
| 1 | 카테고리 생성 → 목록 조회 시 포함 | 200, 조회 시 생성한 카테고리 존재 | `CategoryService.create()` |
| 2 | 카테고리 목록 조회 | 200, SQL로 삽입한 카테고리가 목록에 포함 | `CategoryService.retrieve()` |

### 2. 상품 API

| # | 시나리오 | 기대 결과 | 근거 |
|---|---------|----------|------|
| 1 | 상품 생성 → 목록 조회 시 포함 | 200, 조회 시 생성한 상품 존재 | `ProductService.create()` |
| 2 | 상품 목록 조회 | 200, SQL로 삽입한 상품이 목록에 포함 | `ProductService.retrieve()` |
| 3 | 존재하지 않는 카테고리로 상품 생성 | **not 200** (500) | `categoryRepository.findById().orElseThrow()` |

### 3. 선물 전달 API

| # | 시나리오 | 기대 결과 | 근거 |
|---|---------|----------|------|
| 1 | 정상 선물 전달 + 재고 감소 | 200, 재고가 요청 수량만큼 감소 | `Option.decrease()` |
| 2 | 재고 부족 시 실패 + 재고 유지 | **not 200** (500), 재고 변동 없음 | `Option.decrease()` 예외 |
| 3 | 존재하지 않는 옵션으로 실패 | **not 200** (500) | `optionRepository.findById().orElseThrow()` |

---

## 테스트 데이터 전략

### 공통
- `@Sql`로 `test-data.sql` 실행 (`executionPhase = BEFORE_TEST_METHOD`)
- 각 테스트 전 데이터 초기화 (H2 인메모리 DB 활용)

### test-data.sql 초기 데이터

```sql
-- 회원
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');

-- 카테고리
INSERT INTO category (id, name) VALUES (100, '테스트카테고리');

-- 상품
INSERT INTO product (id, name, price, image_url, category_id)
VALUES (100, '테스트상품', 10000, 'http://test.com/image.jpg', 100);

-- 옵션
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
1. **응답 코드 검증** — 성공 시 200, 실패 시 not 200 (`.statusCode(not(200))`)
2. **다음 행동으로 이전 행동 검증** — 생성 API 호출 후 조회 API로 실제 저장 여부 확인
3. **재고 변동 검증** — 선물 전달 후 DB 직접 조회로 재고 수량 확인

### 주의사항
- 모든 생성 API는 `Content-Type: application/json`으로 JSON 전송
- 선물 전달 시 `Header("Member-Id", memberId)` 필수
- 에러 응답은 모두 500 (별도 에러 핸들러 미구현)