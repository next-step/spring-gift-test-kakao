# 테스트 전략 문서 (TEST_STRATEGY.md)

## 1. 검증할 행위 목록

### 1.1 선택 기준

API E2E 테스트 대상 행위를 선택할 때 다음 기준을 적용한다:

| 우선순위 | 기준 | 설명 |
|----------|------|------|
| P0 | 핵심 성공 경로 | 사용자가 정상적으로 기능을 사용하는 시나리오 |
| P1 | 필수값 검증 | 요청 데이터 누락/오류 시 적절한 에러 반환 |
| P2 | 도메인 규칙 위반 | 비즈니스 로직 위반 시 적절한 에러 반환 |
| P3 | 엣지 케이스 | 경계값, 빈 데이터 등 특수 상황 |

### 1.2 API별 검증 행위

#### POST /api/categories
| 우선순위 | 행위 | 선택 이유 |
|----------|------|-----------|
| P0 | 유효한 이름으로 카테고리 생성 | 핵심 기능, 반드시 동작해야 함 |
| P1 | name 필드 누락 시 400 반환 | 클라이언트 오류 방지 |
| P1 | name 빈 문자열 시 400 반환 | 의미 없는 데이터 방지 |

#### GET /api/categories
| 우선순위 | 행위 | 선택 이유 |
|----------|------|-----------|
| P0 | 카테고리 목록 조회 | 핵심 기능 |
| P3 | 빈 목록 조회 | 초기 상태에서 에러 없이 빈 배열 반환 확인 |

#### POST /api/products
| 우선순위 | 행위 | 선택 이유 |
|----------|------|-----------|
| P0 | 유효한 데이터로 상품 생성 | 핵심 기능 |
| P1 | categoryId 필수값 누락 | FK 관계 검증 |
| P2 | 존재하지 않는 categoryId | 데이터 무결성 보장 |
| P1 | price 음수값 | 비즈니스 규칙 (TODO: 현재 미구현) |

#### GET /api/products
| 우선순위 | 행위 | 선택 이유 |
|----------|------|-----------|
| P0 | 상품 목록 조회 | 핵심 기능 |
| P3 | 빈 목록 조회 | 초기 상태 확인 |
| P0 | 상품-카테고리 관계 포함 조회 | 연관 데이터 정상 반환 확인 |

#### POST /api/gifts
| 우선순위 | 행위 | 선택 이유 |
|----------|------|-----------|
| P0 | 유효한 선물 전송 | 핵심 기능, 재고 차감 포함 |
| P1 | Member-Id 헤더 누락 | 인증 필수 |
| P2 | 존재하지 않는 optionId | 데이터 무결성 |
| P2 | 재고 부족 시 전송 실패 | 핵심 비즈니스 규칙 |
| P2 | 존재하지 않는 memberId | 발신자 검증 |

---

## 2. 테스트 데이터 전략

### 2.1 데이터 준비 방식

#### 선택: API를 통한 데이터 생성 (Black-box 방식)

```java
// 권장: API 호출로 데이터 생성
Long categoryId = given()
    .contentType(ContentType.JSON)
    .body(Map.of("name", "테스트 카테고리"))
.when()
    .post("/api/categories")
.then()
    .extract().path("id");
```

**선택 이유:**
- E2E 테스트의 목적에 부합 (실제 사용자 흐름 재현)
- API 변경 시 테스트도 함께 실패 → 회귀 감지
- 내부 구현(Repository)에 의존하지 않음

#### 대안: Repository 직접 사용 (White-box 방식)

```java
// 특수 케이스: 테스트 전용 데이터가 필요할 때
@Autowired
MemberRepository memberRepository;

@BeforeEach
void setUp() {
    Member member = new Member("테스터", "test@example.com");
    memberRepository.save(member);
}
```

**사용 시점:**
- Member, Option 등 생성 API가 없는 엔티티
- 대량 데이터 필요 시 (성능상 API 호출 비효율)

### 2.2 데이터 정리 방식

#### 선택: @BeforeEach에서 전체 삭제

```java
@BeforeEach
void setUp() {
    RestAssured.port = port;

    // 의존성 역순으로 삭제 (FK 제약 고려)
    giftRepository.deleteAll();      // Gift는 DB 엔티티 아님 (제외)
    wishRepository.deleteAll();
    optionRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    memberRepository.deleteAll();
}
```

**선택 이유:**
- RestAssured는 실제 HTTP 요청 → `@Transactional` 롤백 불가
- 테스트 순서 독립성 보장
- 명시적이고 예측 가능한 초기 상태

#### 삭제 순서 (FK 제약 기준)

```
1. Wish (Member, Product 참조)
2. Option (Product 참조)
3. Product (Category 참조)
4. Category (독립)
5. Member (독립)
```

### 2.3 테스트 데이터 설계 원칙

| 원칙 | 설명 | 예시 |
|------|------|------|
| 최소 데이터 | 테스트에 필요한 최소한만 생성 | 목록 조회 시 2-3개면 충분 |
| 의미 있는 값 | 테스트 의도가 드러나는 값 사용 | `"재고부족테스트용"` |
| 고유 식별 가능 | 테스트별로 구분 가능한 값 | 타임스탬프 또는 테스트명 포함 |
| 경계값 포함 | 0, 1, MAX 등 경계값 테스트 | `quantity: 0`, `quantity: 1` |

---

## 3. 검증 전략

### 3.1 검증 계층

E2E 테스트에서 검증하는 항목을 3계층으로 구분한다:

```
┌─────────────────────────────────────┐
│  Layer 1: HTTP 응답 검증            │  ← 모든 테스트 필수
│  - 상태 코드 (200, 201, 400, 500)   │
│  - Content-Type                      │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Layer 2: 응답 본문 검증            │  ← 대부분 테스트 필수
│  - 필수 필드 존재                    │
│  - 필드 값 정확성                    │
│  - 배열 크기                         │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Layer 3: 상태 변화 검증            │  ← 상태 변경 API만
│  - DB 데이터 변경                    │
│  - 재고 차감                         │
└─────────────────────────────────────┘
```

### 3.2 API별 검증 포인트

#### POST /api/categories (생성)

```java
// Layer 1: HTTP 응답
.statusCode(201)

// Layer 2: 응답 본문
.body("id", notNullValue())
.body("name", equalTo("전자기기"))

// Layer 3: 상태 변화
// → GET /api/categories 호출하여 목록에 포함 확인
```

#### GET /api/categories (조회)

```java
// Layer 1: HTTP 응답
.statusCode(200)

// Layer 2: 응답 본문
.body("size()", equalTo(2))
.body("[0].id", notNullValue())
.body("[0].name", notNullValue())
```

#### POST /api/products (생성)

```java
// Layer 1: HTTP 응답
.statusCode(201)

// Layer 2: 응답 본문
.body("id", notNullValue())
.body("name", equalTo("무선 이어폰"))
.body("price", equalTo(150000))
.body("category.id", equalTo(categoryId.intValue()))

// Layer 3: 상태 변화
// → GET /api/products로 확인
```

#### POST /api/gifts (선물 전송)

```java
// Layer 1: HTTP 응답
.statusCode(200)

// Layer 2: 응답 본문 (void 반환이므로 생략)

// Layer 3: 상태 변화 (핵심!)
// → Option 재고 감소 확인 필수
int remainingQuantity = optionRepository.findById(optionId)
    .orElseThrow().getQuantity();
assertThat(remainingQuantity).isEqualTo(initialQuantity - giftQuantity);
```

### 3.3 실패 케이스 검증

```java
// 존재하지 않는 리소스
.statusCode(500)  // TODO: 404로 개선 필요

// 비즈니스 규칙 위반 (재고 부족)
.statusCode(500)  // TODO: 400으로 개선 필요

// 필수값 누락 (validation 구현 후)
.statusCode(400)
.body("errors", hasSize(greaterThan(0)))
```

### 3.4 검증 헬퍼 패턴

```java
// 공통 성공 응답 검증
ResponseSpecification successSpec = new ResponseSpecBuilder()
    .expectStatusCode(200)
    .expectContentType(ContentType.JSON)
    .build();

// 공통 생성 응답 검증
ResponseSpecification createdSpec = new ResponseSpecBuilder()
    .expectStatusCode(201)
    .expectBody("id", notNullValue())
    .build();
```

---

## 4. 주요 의사결정

### 4.1 테스트 도구 선택: RestAssured vs MockMvc

| 항목 | RestAssured | MockMvc |
|------|-------------|---------|
| 테스트 범위 | 전체 HTTP 스택 | 서블릿 레이어까지 |
| 실제 서버 | 실제 기동 | Mock 서버 |
| 속도 | 상대적으로 느림 | 빠름 |
| 가독성 | BDD 스타일, 직관적 | 체이닝 방식 |
| 트랜잭션 롤백 | 불가 | 가능 |

**결정: RestAssured 사용**

**이유:**
1. 실제 운영 환경과 동일한 조건에서 테스트
2. 네트워크 레이어 포함 (직렬화/역직렬화, 필터 등)
3. Given-When-Then 문법으로 가독성 향상
4. API 문서화 도구와 연계 용이

### 4.2 테스트 격리: @Transactional vs deleteAll

| 항목 | @Transactional | deleteAll |
|------|----------------|-----------|
| 적용 대상 | MockMvc | RestAssured (필수) |
| 성능 | 빠름 (롤백만) | 느림 (실제 삭제) |
| 명시성 | 암묵적 | 명시적 |
| FK 제약 | 자동 처리 | 순서 관리 필요 |

**결정: @BeforeEach + deleteAll**

**이유:**
1. RestAssured는 별도 트랜잭션으로 실행되어 롤백 불가
2. 명시적인 정리로 디버깅 용이
3. 테스트 순서 독립성 보장

### 4.3 데이터 생성: API 호출 vs Repository 직접 사용

**결정: 하이브리드 방식**

| 엔티티 | 방식 | 이유 |
|--------|------|------|
| Category | API 호출 | 생성 API 존재, E2E 검증 가능 |
| Product | API 호출 | 생성 API 존재, E2E 검증 가능 |
| Member | Repository | 생성 API 없음 |
| Option | Repository | 생성 API 없음 |

### 4.4 예외 상황 처리 전략

**현재 상태:**
- `NoSuchElementException` → 500 Internal Server Error
- `IllegalStateException` (재고 부족) → 500 Internal Server Error

**결정: 현재 상태로 테스트 작성 후 개선**

**이유:**
1. 테스트 우선: 현재 동작을 기록하는 테스트 먼저 작성
2. 리팩토링 안전망 확보 후 예외 처리 개선
3. 개선 시 테스트 기대값만 변경 (400, 404 등)

```java
// 현재
.statusCode(500)

// 개선 후 (TODO)
.statusCode(404)    
.body("message", containsString("not found"))
```

### 4.5 테스트 명명 규칙

**결정: 한글 메서드명 사용**

```java
@Test
void 카테고리_생성_성공() { }

@Test
void 상품_생성_실패_존재하지_않는_카테고리() { }

@Test
void 선물_전송_실패_재고_부족() { }
```

**이유:**
1. 테스트 의도가 명확하게 드러남
2. 테스트 리포트 가독성 향상
3. 비개발자도 테스트 결과 이해 가능
---
