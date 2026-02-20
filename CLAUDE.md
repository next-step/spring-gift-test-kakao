# CLAUDE.md - Spring Gift Service 테스트 가이드

## 1. Mission

이 저장소에서 Claude의 1차 목표는 **API 5개 E2E 테스트 완성 + 안전한 리팩토링**이다.

### 대상 API
| # | Method | Endpoint | 설명 |
|---|--------|----------|------|
| 1 | POST | /api/categories | 카테고리 생성 |
| 2 | GET | /api/categories | 카테고리 목록 조회 |
| 3 | POST | /api/products | 상품 생성 |
| 4 | GET | /api/products | 상품 목록 조회 |
| 5 | POST | /api/gifts | 선물 전송 |

### 핵심 원칙
- 기능 동작 보존(회귀 방지)이 최우선
- 테스트 없이 프로덕션 코드 변경 금지
- 점진적 개선: 한 번에 하나씩

---

## 2. Test Stack & Rules

### 기술 스택
- **Spring Boot Test** (`@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`)
- **RestAssured** (E2E 테스트용 HTTP 클라이언트)
- **H2 Database** (in-memory, 테스트 격리)
- **JUnit 5** + **AssertJ**

### RestAssured 설정
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcceptanceTest {
    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}
```

### RestAssured 사용 예시
```java
// POST 요청
given()
    .contentType(ContentType.JSON)
    .body(requestBody)
.when()
    .post("/api/categories")
.then()
    .statusCode(201)
    .body("name", equalTo("전자기기"));

// GET 요청
given()
.when()
    .get("/api/products")
.then()
    .statusCode(200)
    .body("size()", greaterThan(0));

// 헤더 포함 요청
given()
    .contentType(ContentType.JSON)
    .header("Member-Id", 1L)
    .body(giftRequest)
.when()
    .post("/api/gifts")
.then()
    .statusCode(200);
```

### 테스트 구조 규칙
```java
@Test
void 테스트_메서드명_한글_허용() {
    // given - 테스트 데이터 준비

    // when - API 호출

    // then - 검증
}
```

### 데이터 격리 규칙
- `@BeforeEach`에서 `repository.deleteAll()` 호출 (RestAssured는 실제 HTTP 요청이므로 `@Transactional` 롤백 불가)
- 또는 `@DirtiesContext`로 컨텍스트 재생성
- 테스트 간 데이터 의존성 절대 금지

### 픽스처 규칙
- 하드코딩 최소화
- 테스트 헬퍼 메서드로 데이터 생성 추출
- 예: `createCategory(String name)`, `createProduct(String name, Long categoryId)`

---

## 3. E2E Scenario Matrix

### 3.1 POST /api/categories

| 시나리오 | 검증 포인트 |
|----------|-------------|
| ✅ 성공: 유효한 이름 | 201 Created, 응답에 id/name 포함, DB 저장 확인 |
| ❌ 실패: name 누락 | 400 Bad Request (TODO: 현재 validation 미구현) |
| ❌ 실패: name 빈 문자열 | 400 Bad Request (TODO: 현재 validation 미구현) |

**현재 코드 이슈**: `@RequestBody` 누락 → 테스트 시 발견 예상

### 3.2 GET /api/categories

| 시나리오 | 검증 포인트 |
|----------|-------------|
| ✅ 성공: 빈 목록 | 200 OK, 빈 배열 반환 |
| ✅ 성공: N개 존재 | 200 OK, N개 요소 배열, 각 요소에 id/name |

### 3.3 POST /api/products

| 시나리오 | 검증 포인트 |
|----------|-------------|
| ✅ 성공: 유효한 요청 | 201 Created, 응답에 id/name/price/imageUrl 포함 |
| ❌ 실패: 존재하지 않는 categoryId | 500 (NoSuchElementException) → TODO: 404로 개선 |
| ❌ 실패: 필수값 누락 | 400 Bad Request (TODO: validation 미구현) |

**현재 코드 이슈**: `@RequestBody` 누락 → 테스트 시 발견 예상

**Request 필드**:
- `name`: String (필수)
- `price`: int (필수)
- `imageUrl`: String
- `categoryId`: Long (필수, FK)

### 3.4 GET /api/products

| 시나리오 | 검증 포인트 |
|----------|-------------|
| ✅ 성공: 빈 목록 | 200 OK, 빈 배열 |
| ✅ 성공: N개 존재 | 200 OK, 각 상품에 id/name/price/imageUrl/category 포함 |

### 3.5 POST /api/gifts

| 시나리오 | 검증 포인트 |
|----------|-------------|
| ✅ 성공: 유효한 선물 전송 | 200 OK, Option 재고 감소 확인 |
| ❌ 실패: Member-Id 헤더 누락 | 400 Bad Request |
| ❌ 실패: 존재하지 않는 optionId | 500 (NoSuchElementException) |
| ❌ 실패: 재고 부족 | 500 (IllegalStateException) → TODO: 400으로 개선 |
| ❌ 실패: 발신자(memberId) 미존재 | 500 (FakeGiftDelivery에서 발생) |

**Request 필드**:
- `optionId`: Long (필수)
- `quantity`: int (필수)
- `receiverId`: Long (필수)
- `message`: String

**Header**:
- `Member-Id`: Long (필수)

---

## 4. Refactoring Strategy

### 1단계: 중복 제거
- 요청 JSON 생성 → `TestRequestBuilder` 또는 헬퍼 메서드
- 데이터 세팅 → `TestFixtures` 클래스
- 공통 assert → RestAssured `ResponseSpecification` 활용

### 2단계: 가독성 개선
- 메서드 추출: `givenCategory()`, `whenCreateProduct()`, `thenProductCreated()`
- 네이밍: `테스트대상_상황_기대결과` 형식

### 3단계: 안전장치 유지
- 리팩토링 전후 반드시 테스트 통과 확인
- `./gradlew test` 실행 필수

### 금지 사항
- ❌ 테스트 없는 프로덕션 코드 변경
- ❌ 한 번에 대규모 구조 변경
- ❌ 테스트 코드에서 프로덕션 로직 복제

---

## 5. Working Agreement

### 작업 사이클 (Red-Green-Refactor)
```
1. 테스트 추가 (Red)
2. 테스트 통과 확인 (Green)
3. 리팩토링
4. 재통과 확인
```

### 실패 처리 규칙
테스트 실패 시 다음을 기록:
- **원인**: 왜 실패했는가
- **재현**: 어떤 조건에서 발생하는가
- **수정**: 어떻게 고쳤는가

### 커밋 메시지 규칙
```
test: Category API E2E 테스트 추가
test: Product 생성 실패 케이스 추가
refactor: 테스트 픽스처 공통화
fix: @RequestBody 누락 수정
```

---

## 6. Definition of Done

### 필수 완료 조건
- [ ] API 5개 모두 E2E 성공 케이스 통과
- [ ] 각 API 최소 1개 이상 실패 케이스 포함
- [ ] 도메인 규칙 위반 케이스 포함 (예: 재고 부족)
- [ ] 중복 테스트 코드 헬퍼/픽스처로 정리
- [ ] `./gradlew test` 전체 통과
- [ ] README.md에 테스트 실행 방법 반영

### 품질 기준
- 테스트 독립성: 순서 무관하게 개별 실행 가능
- 테스트 명확성: 테스트명만 보고 의도 파악 가능
- 테스트 신뢰성: Flaky 테스트 0개

---

## 7. First Execution Plan

### Sprint 1: 기반 구축 + Category API

#### TODO 1.1: 테스트 기반 구조 생성
- [ ] `src/test/java/gift/` 디렉토리 생성
- [ ] `build.gradle`에 RestAssured 의존성 추가: `testImplementation 'io.rest-assured:rest-assured'`
- [ ] `AcceptanceTestBase.java` 생성 (RestAssured 포트 설정, 공통 유틸)
- [ ] 테스트용 `application.properties` 설정 (H2)

#### TODO 1.2: Category API E2E
- [ ] `CategoryAcceptanceTest.java` 생성
- [ ] `POST /api/categories` 성공 테스트
- [ ] `GET /api/categories` 빈 목록 테스트
- [ ] `GET /api/categories` 데이터 존재 테스트
- [ ] **발견된 버그 수정**: `@RequestBody` 누락

#### TODO 1.3: Product API E2E
- [ ] `ProductAcceptanceTest.java` 생성
- [ ] `POST /api/products` 성공 테스트 (Category 선행 생성)
- [ ] `POST /api/products` 실패: 존재하지 않는 categoryId
- [ ] `GET /api/products` 테스트
- [ ] **발견된 버그 수정**: `@RequestBody` 누락

### Sprint 2: Gift API + 리팩토링

#### TODO 2.1: Gift API E2E (의존성 많음)
- [ ] `GiftAcceptanceTest.java` 생성
- [ ] 테스트 데이터 준비: Member, Category, Product, Option
- [ ] `POST /api/gifts` 성공 테스트
- [ ] `POST /api/gifts` 실패: Member-Id 헤더 누락
- [ ] `POST /api/gifts` 실패: 존재하지 않는 optionId
- [ ] `POST /api/gifts` 실패: 재고 부족

#### TODO 2.2: 공통 fixture/helper 리팩토링
- [ ] `TestFixtures.java` 추출 (엔티티 생성 헬퍼)
- [ ] `ApiTestHelper.java` 추출 (RestAssured 요청 빌더)
- [ ] 중복 코드 제거

#### TODO 2.3: 최종 검증
- [ ] 전체 테스트 실행 (`./gradlew test`)
- [ ] README.md 테스트 실행 방법 추가
- [ ] 코드 리뷰 및 정리

---

## 부록: 현재 코드 구조

```
src/main/java/gift/
├── Application.java
├── ui/
│   ├── CategoryRestController.java  ← @RequestBody 누락
│   ├── ProductRestController.java   ← @RequestBody 누락
│   └── GiftRestController.java      ← 정상
├── application/
│   ├── CategoryService.java
│   ├── ProductService.java
│   ├── GiftService.java
│   ├── Create*Request.java
│   └── GiveGiftRequest.java
├── model/
│   ├── Category.java
│   ├── Product.java
│   ├── Option.java (decrease 메서드: 재고 부족 시 IllegalStateException)
│   ├── Member.java
│   ├── Gift.java
│   └── *Repository.java
└── infrastructure/
    └── FakeGiftDelivery.java (Member 조회 실패 시 예외)
```
