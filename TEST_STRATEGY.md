# TEST_STRATEGY.md — 인수 테스트 전략

## 1. 검증할 행위 목록

### 선택 기준

- **사용자가 실제로 수행하는 행위**인가? (API 엔드포인트가 존재하는가)
- **시스템의 핵심 가치**와 직결되는가? (선물 전달, 재고 관리)
- **실패 시 비즈니스 영향**이 큰가? (재고 부족인데 선물이 나감 → 치명적)

### 행위 목록

#### 카테고리 (2개 행위)

| ID | 행위 | 엔드포인트 | 선택 이유 |
|----|------|-----------|-----------|
| C1 | 카테고리를 생성한다 | `POST /api/categories` | 상품 생성의 전제 조건. 생성 없이는 상품을 만들 수 없다 |
| C2 | 카테고리 목록을 조회한다 | `GET /api/categories` | 생성한 카테고리가 실제로 존재하는지 확인. C1의 결과 검증 수단 |

#### 상품 (2개 행위)

| ID | 행위 | 엔드포인트 | 선택 이유 |
|----|------|-----------|-----------|
| P1 | 상품을 생성한다 | `POST /api/products` | 선물하기의 전제 조건. 카테고리와 연결되는 핵심 도메인 |
| P2 | 상품 목록을 조회한다 | `GET /api/products` | 생성한 상품이 올바른 카테고리와 함께 조회되는지 확인 |

#### 선물하기 (5개 행위) — 핵심 비즈니스

| ID | 행위 | 엔드포인트 | 선택 이유 |
|----|------|-----------|-----------|
| G1 | 선물을 보낸다 (재고 충분) | `POST /api/gifts` | 핵심 성공 시나리오. 시스템 존재 이유 |
| G2 | 선물을 보내면 재고가 감소한다 | `POST /api/gifts` ×2 | **핵심**. 재고 전부 소진 후 재시도 → 실패로 검증 (행위 기반) |
| G3 | 재고보다 많은 수량을 선물하면 실패한다 | `POST /api/gifts` | 재고 초과 방지. 실패해야 하는 상황에서 반드시 실패해야 함 |
| G4 | 존재하지 않는 옵션으로 선물하면 실패한다 | `POST /api/gifts` | 잘못된 입력에 대한 방어 |
| G5 | Member-Id 헤더 없이 선물하면 실패한다 | `POST /api/gifts` | 필수 헤더 누락에 대한 방어 |

#### 제외한 행위와 이유

| 행위 | 제외 이유 |
|------|-----------|
| 위시리스트 추가 (WishService) | 컨트롤러가 존재하지 않음. 사용자가 HTTP로 접근할 수 없으므로 "사용자 관점의 행위"가 아님. API가 추가되면 그때 인수 테스트 작성 |
| 옵션 생성 (OptionService) | 컨트롤러가 존재하지 않음. 동일한 이유로 제외. 선물하기 테스트의 사전 데이터로만 활용 |

---

## 2. 테스트 데이터 전략

### 원칙

1. **구현 비의존**: Java 엔티티/repository를 직접 사용하지 않는다. SQL 스크립트로 데이터를 준비한다.
2. **매 테스트 격리**: 매 테스트 실행 전 전체 데이터를 초기화하고 다시 세팅한다.
3. **의도가 명확한 데이터**: 테스트 목적에 맞는 최소한의 데이터만 준비한다.

### 방식: @Sql 스크립트

```java
@Sql(scripts = "classpath:cleanup.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data.sql", executionPhase = BEFORE_TEST_METHOD)
```

`RANDOM_PORT` 환경에서 `@Transactional` 롤백은 **동작하지 않는다** (HTTP 요청이 별도 스레드에서 처리되므로). 따라서 `@Sql`로 매 테스트 전에 초기화한다.

### cleanup.sql — 데이터 초기화

FK 제약 조건 순서를 고려하여 자식 테이블부터 삭제한다.

```sql
DELETE FROM wish;
DELETE FROM option;
DELETE FROM product;
DELETE FROM member;
DELETE FROM category;
```

> H2에서 TRUNCATE 사용 시 FK 제약으로 실패할 수 있으므로 DELETE를 사용한다.
> `ALTER TABLE ... ALTER COLUMN id RESTART WITH 1`로 ID를 초기화하면 SQL에서 지정한 ID와 충돌하지 않는다.

### test-data.sql — 기본 테스트 데이터

```sql
-- 카테고리
INSERT INTO category (id, name) VALUES (1, '간식');
INSERT INTO category (id, name) VALUES (2, '음료');

-- 상품
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 5000, 'http://img.com/choco.png', 1);
INSERT INTO product (id, name, price, image_url, category_id) VALUES (2, '커피', 3000, 'http://img.com/coffee.png', 2);

-- 옵션 (재고 관리 대상)
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '초콜릿 기본', 10, 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (2, '커피 기본', 1, 2);

-- 회원
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
```

### 데이터 설계 의도

| 데이터 | 값 | 의도 |
|--------|-----|------|
| 옵션1 재고 | 10개 | G1(성공) 후 G2(재고 소진 검증)에 사용. 10개 전부 보내고 → 1개 더 → 실패 |
| 옵션2 재고 | 1개 | G3(재고 초과) 테스트에 사용. 2개 이상 요청 시 즉시 실패 |
| 회원 2명 | sender, receiver | 선물하기에 필요한 최소 구성 |
| 카테고리 2개 | 간식, 음료 | 목록 조회 시 복수 결과 검증 가능 |
| 상품 2개 | 각 카테고리에 1개씩 | 카테고리-상품 관계 검증 가능 |

### 시나리오별 데이터 요구사항

| 테스트 | 필요한 사전 데이터 | 비고 |
|--------|---------------------|------|
| C1 (카테고리 생성) | 없음 | API로 직접 생성 |
| C2 (카테고리 조회) | category 2건 | test-data.sql로 준비 |
| P1 (상품 생성) | category 1건 이상 | test-data.sql의 카테고리 활용 |
| P2 (상품 조회) | product + category | test-data.sql로 준비 |
| G1 (선물 성공) | option + member 2명 | test-data.sql 전체 필요 |
| G2 (재고 감소) | option(재고 10) + member 2명 | 옵션1 사용 |
| G3 (재고 초과 실패) | option(재고 1) + member 2명 | 옵션2 사용 |
| G4 (존재하지 않는 옵션) | member 2명 | 존재하지 않는 옵션 ID 사용 |
| G5 (헤더 누락) | option + member | Member-Id 헤더 제외 |

---

## 3. 검증 전략

### 원칙: "어떻게 되는가"를 검증한다

- 내부 구현(repository, 엔티티, 서비스 내부 로직)에 의존하지 않는다.
- **HTTP 응답**과 **후속 행위의 성공/실패**로만 검증한다.
- DB를 직접 조회하지 않는다.
- 리팩토링해도 깨지지 않는 테스트를 목표로 한다.

### 검증 패턴

#### 패턴 1: 응답 검증
HTTP 응답의 상태 코드와 본문으로 직접 검증한다.

```
POST /api/categories → 200 + 응답 body에 id, name 존재
GET /api/categories → 200 + 응답 body가 배열이고 기대한 항목 포함
```

#### 패턴 2: 시나리오 체이닝 (다음 행동으로 이전 행동을 검증)
생성 API 호출 후 조회 API로 결과를 확인한다.

```
POST /api/categories (생성) → GET /api/categories (조회) → 방금 생성한 카테고리 존재
```

#### 패턴 3: 실패로 상태 변화를 검증
재고 감소처럼 DB 직접 조회 없이는 확인하기 어려운 상태 변화를 후속 행위의 실패로 검증한다.

```
POST /api/gifts (재고 10개 전부 소진) → 200
POST /api/gifts (같은 옵션에 1개 더) → 500 (재고 부족)
→ 두 번째 실패가 곧 "재고가 감소했다"는 증거
```

### 행위별 검증 방법

| ID | 행위 | 검증 패턴 | 구체적 검증 내용 |
|----|------|-----------|-----------------|
| C1 | 카테고리 생성 | 응답 + 체이닝 | 200 응답, body에 id/name 존재. 이후 GET으로 목록에 포함 확인 |
| C2 | 카테고리 조회 | 응답 | 200 응답, body가 배열, test-data의 2건 포함 |
| P1 | 상품 생성 | 응답 + 체이닝 | 200 응답, body에 id/name/price/category 존재. 이후 GET으로 확인 |
| P2 | 상품 조회 | 응답 | 200 응답, body가 배열, 상품에 category 정보 포함 |
| G1 | 선물 성공 | 응답 | 200 응답 (body 없음) |
| G2 | 재고 감소 | 실패로 검증 | 재고 전부 소진(200) → 추가 요청(500). 두 번째 실패 = 재고 감소 증거 |
| G3 | 재고 초과 실패 | 응답 | 500 응답 |
| G4 | 없는 옵션 | 응답 | 500 응답 |
| G5 | 헤더 누락 | 응답 | 400 응답 |

---

## 4. 주요 의사결정

### 결정 1: RestAssured 사용 (TestRestTemplate, MockMvc 대신)

**선택**: RestAssured

**이유**:
- 실제 HTTP 요청을 보내므로 인수 테스트 취지에 부합
- 메서드 체이닝으로 Given-When-Then 구조가 코드에 자연스럽게 드러남
- MockMvc는 서블릿 컨테이너를 띄우지 않아 "진짜 HTTP" 테스트가 아님
- TestRestTemplate보다 가독성이 좋고 검증 표현이 풍부함

### 결정 2: @Sql 스크립트로 데이터 준비 (repository 직접 사용 대신)

**선택**: `@Sql` 스크립트

**이유**:
- repository를 사용하면 Java 엔티티 생성자/필드에 의존 → 리팩토링 시 테스트 깨짐
- SQL은 테이블 스키마에만 의존하므로 Java 코드 변경에 영향받지 않음
- "어떻게 되는가" 원칙에 부합 — 테스트가 내부 구현을 모른다
- 테스트 데이터가 한눈에 보여 가독성이 좋다

**트레이드오프**: JPA 네이밍 전략(camelCase → snake_case)을 SQL에서 따라야 함

### 결정 3: DB 직접 조회 대신 행위 기반 검증

**선택**: 후속 API 호출로 상태 변화 검증

**이유**:
- `optionRepository.findById()`로 재고를 직접 확인하면 구현에 의존
- 재고 감소는 "전부 소진 → 재시도 실패" 패턴으로 행위만으로 검증 가능
- 이 방식은 내부 저장 구조가 바뀌어도(예: Redis 캐시 도입) 테스트가 깨지지 않음

**트레이드오프**: "정확히 몇 개 남았는지"는 검증할 수 없음. 하지만 인수 테스트에서 그 수준의 정밀도는 불필요하다고 판단

### 결정 4: WishService를 인수 테스트 범위에서 제외

**선택**: 제외

**이유**:
- 컨트롤러가 없으므로 사용자가 HTTP로 접근할 수 없음
- 인수 테스트는 "사용자 관점의 행위 검증"이 목적
- WishService를 테스트하려면 서비스 빈을 직접 주입해야 하는데, 이는 구현 의존
- API가 추가되는 시점에 인수 테스트를 작성하는 것이 적절

### 결정 5: RANDOM_PORT에서 @Transactional 롤백 불가

**사실**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured 조합에서 `@Transactional` 롤백은 동작하지 않는다.

**대응**: 매 테스트 전 `@Sql(cleanup.sql, BEFORE_TEST_METHOD)`로 전체 데이터 삭제 후 재세팅

### 결정 6: 컨트롤러별 요청 바인딩 방식 차이 대응

**사실**:
- `POST /api/products`, `POST /api/categories` → `@RequestBody` 없음 → form params
- `POST /api/gifts` → `@RequestBody` 있음 → JSON body

**대응**: RestAssured에서 contentType과 body 전달 방식을 엔드포인트에 맞게 구분

---

## 5. 테스트 파일 구조

```
src/test/
├── java/gift/
│   ├── CategoryAcceptanceTest.java    # C1, C2
│   ├── ProductAcceptanceTest.java     # P1, P2
│   └── GiftAcceptanceTest.java        # G1, G2, G3, G4, G5
└── resources/
    ├── cleanup.sql                     # 매 테스트 전 데이터 초기화
    └── test-data.sql                   # 기본 테스트 데이터
```
