# /test-behavior — 행위 기반 인수 테스트 작성

사용자가 검증할 행위를 설명하면, CLAUDE.md의 테스트 가이드에 따라 인수 테스트 코드를 작성한다.

## 실행 전 확인

1. `build.gradle`에 RestAssured 의존성이 있는지 확인. 없으면 추가:
   ```groovy
   testImplementation 'io.rest-assured:rest-assured'
   ```
2. `src/test/resources/cleanup.sql`이 존재하는지 확인. 없으면 생성.
3. 테스트에 필요한 사전 데이터 SQL(`src/test/resources/` 하위)이 있는지 확인. 없으면 생성.

## 테스트 코드 작성 규칙

### 원칙: "어떻게 되는가"를 검증한다
- 내부 구현(repository, 엔티티, 서비스 로직)에 직접 의존하지 않는다.
- **사용자 입력(HTTP 요청) → 결과(HTTP 응답 또는 후속 행위의 성공/실패)**로만 검증한다.
- DB를 직접 조회해서 상태를 확인하지 않는다.
- 리팩토링해도 깨지지 않는 테스트를 목표로 한다.

### 검증 패턴: 다음 행동으로 이전 행동을 검증
- 생성 → 조회에서 확인 (시나리오 체이닝)
- 재고 소진 → 재시도 시 실패로 재고 감소 검증

### 테스트 클래스 구조

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SomeAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 행위를_한글로_서술한다() {
        // given — 사전 조건 (필요 시 API 호출로 준비)
        // when — 검증 대상 행위 실행
        // then — 결과 확인 (HTTP 응답 또는 후속 행위)
    }
}
```

### RestAssured 요청 작성

#### form params 방식 (Product, Category 생성)
`POST /api/products`와 `POST /api/categories`는 `@RequestBody`가 없다. form params로 전송해야 한다.

```java
ExtractableResponse<Response> response = RestAssured.given().log().all()
        .contentType(ContentType.FORM)
        .formParam("name", "테스트 상품")
        .formParam("price", 10000)
        .formParam("imageUrl", "http://example.com/img.png")
        .formParam("categoryId", 1)
        .when().post("/api/products")
        .then().log().all().extract();
```

#### JSON body 방식 (Gift 생성)
`POST /api/gifts`는 `@RequestBody`가 있다. JSON body로 전송해야 한다.

```java
ExtractableResponse<Response> response = RestAssured.given().log().all()
        .contentType(ContentType.JSON)
        .header("Member-Id", senderId)
        .body(Map.of(
                "optionId", optionId,
                "quantity", 1,
                "receiverId", receiverId,
                "message", "선물 메시지"
        ))
        .when().post("/api/gifts")
        .then().log().all().extract();
```

### 테스트 데이터 전략

#### SQL 스크립트 사용 (@Sql)
- `cleanup.sql` — 매 테스트 전 모든 테이블 초기화 (FK 역순으로 DELETE)
- `test-data.sql` — 테스트에 필요한 기본 데이터 INSERT
- H2 컬럼명은 JPA 기본 네이밍 전략: camelCase → snake_case (예: `imageUrl` → `image_url`)

#### cleanup.sql 예시
```sql
DELETE FROM wish;
DELETE FROM gift;
DELETE FROM option;
DELETE FROM product;
DELETE FROM member;
DELETE FROM category;
```

#### 시나리오별 추가 데이터가 필요한 경우
- 별도 SQL 파일을 만들어 테스트 메서드/클래스에 `@Sql`을 추가로 지정한다.
- 또는 테스트 내에서 API 호출로 데이터를 준비한다 (인수 테스트 취지에 부합).

### 테스트 격리
- `RANDOM_PORT`에서 `@Transactional` 롤백은 **동작하지 않는다**. 별도 스레드에서 HTTP 요청을 처리하므로 테스트 트랜잭션과 분리됨.
- 반드시 `@Sql(cleanup.sql, BEFORE_TEST_METHOD)`로 매 테스트 전 데이터를 초기화한다.

### 테스트 메서드 네이밍
- 한글로 행위를 서술한다: `카테고리를_생성한다()`, `선물하기_재고_부족_시_실패한다()`
- Given-When-Then 구조를 주석으로 명시한다.

## 실행 예시

사용자: "상품 생성 테스트 작성해줘"

→ 수행할 작업:
1. cleanup.sql / test-data.sql 존재 여부 확인 (없으면 생성)
2. `ProductAcceptanceTest` 클래스 생성 (또는 기존 파일에 추가)
3. 상품 생성 + 목록 조회로 검증하는 테스트 작성
4. `./gradlew test --tests "gift.ProductAcceptanceTest"` 실행하여 통과 확인
