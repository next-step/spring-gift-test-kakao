# /test-behavior — Cucumber/Gherkin 기반 인수 테스트 작성

사용자가 검증할 행위를 설명하면, Gherkin 시나리오(.feature)와 Step Definition을 작성한다.

## 실행 전 확인

1. `build.gradle`에 Cucumber + RestAssured 의존성이 있는지 확인. 없으면 추가:
   ```groovy
   testImplementation 'io.rest-assured:rest-assured'
   testImplementation 'io.cucumber:cucumber-java:7.22.1'
   testImplementation 'io.cucumber:cucumber-spring:7.22.1'
   testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.22.1'
   testImplementation 'org.junit.platform:junit-platform-suite'
   ```
2. Cucumber 인프라 파일 존재 여부 확인. 없으면 생성:
   - `src/test/java/gift/cucumber/CucumberTest.java` — @Suite 엔트리포인트
   - `src/test/java/gift/cucumber/CucumberSpringConfig.java` — @CucumberContextConfiguration + @SpringBootTest
3. `src/test/resources/features/` 디렉토리 존재 여부 확인. 없으면 생성.
4. `src/test/resources/cleanup.sql` 존재 여부 확인. 없으면 생성.

## Gherkin 시나리오 작성 규칙

### 원칙: 비즈니스 언어로 작성한다
- **기획자/QA가 읽고 이해할 수 있어야 한다.**
- HTTP 메서드, 상태 코드, JSON 필드명, URL 경로 등 구현 세부사항을 시나리오에 쓰지 않는다.
- 한국어 Gherkin 키워드를 사용한다: `기능`, `시나리오`, `주어진`/`그리고`/`만일`/`그러면`

### 좋은 예시
```gherkin
# language: ko
기능: 선물하기

  시나리오: 재고가 충분하면 선물하기에 성공한다
    주어진 "교환권" 카테고리가 등록되어 있다
    그리고 "교환권" 카테고리에 "아이스 아메리카노" 상품이 등록되어 있다
    그리고 "아이스 아메리카노" 상품에 재고가 5개인 "톨 사이즈" 옵션이 있다
    그리고 회원 "철수"와 "영희"가 등록되어 있다
    만일 "철수"가 "영희"에게 "아이스 아메리카노"의 "톨 사이즈" 1개를 선물한다
    그러면 선물하기가 성공한다
```

### 나쁜 예시
```gherkin
  시나리오: 선물 API 호출
    Given POST /api/gifts에 JSON body를 전송한다
    Then HTTP 200 응답을 받는다
```

### "어떻게 되는가"를 검증한다
- 내부 구현(repository, 엔티티, 서비스 로직)에 직접 의존하지 않는다.
- **사용자 입력 → 결과(성공/실패)**로만 검증한다.
- DB를 직접 조회해서 상태를 확인하지 않는다.

### 검증 패턴: 다음 행동으로 이전 행동을 검증
- 생성 → 조회에서 확인 (시나리오 체이닝)
- 재고 소진 → 재시도 시 실패로 재고 감소 검증

## Step Definition 작성 규칙

### 파일 구조
```
src/test/java/gift/cucumber/
├── CucumberTest.java
├── CucumberSpringConfig.java
└── steps/
    ├── CategorySteps.java
    ├── ProductSteps.java
    └── GiftSteps.java
```

### Step Definition 클래스 구조
```java
public class GiftSteps {

    @LocalServerPort
    int port;

    private ExtractableResponse<Response> response;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }

    @주어진("회원 {string}와 {string}가 등록되어 있다")
    public void 회원이_등록되어_있다(String sender, String receiver) {
        // SQL 스크립트 또는 API 호출로 데이터 준비
    }

    @만일("{string}가 {string}에게 {string}의 {string} {int}개를 선물한다")
    public void 선물한다(String sender, String receiver, String product, String option, int qty) {
        response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of("optionId", optionId, "quantity", qty,
                             "receiverId", receiverId, "message", "선물"))
                .when().post("/api/gifts")
                .then().log().all().extract();
    }

    @그러면("선물하기가 성공한다")
    public void 선물하기_성공() {
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
```

### RestAssured 요청 작성

모든 POST 엔드포인트에 `@RequestBody`가 있으므로 **JSON body**로 전송한다.

```java
// 카테고리 생성
RestAssured.given().log().all()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "카테고리명"))
        .when().post("/api/categories")
        .then().log().all().extract();

// 상품 생성
RestAssured.given().log().all()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "상품명", "price", 10000,
                     "imageUrl", "http://example.com/img.png", "categoryId", 1))
        .when().post("/api/products")
        .then().log().all().extract();

// 선물하기
RestAssured.given().log().all()
        .contentType(ContentType.JSON)
        .header("Member-Id", senderId)
        .body(Map.of("optionId", optionId, "quantity", 1,
                     "receiverId", receiverId, "message", "선물 메시지"))
        .when().post("/api/gifts")
        .then().log().all().extract();
```

### 기존 헬퍼 재사용
- `AcceptanceTestSupport` 클래스의 공통 헬퍼 메서드를 Step Definition에서 활용할 수 있다.
- 필요 시 헬퍼를 추가/확장한다.

## 테스트 데이터 전략

### Cucumber에서의 데이터 준비
- `@Before` 훅에서 cleanup SQL을 실행하거나, Step Definition 내에서 API 호출로 데이터를 준비한다.
- `Given` 스텝에서 API 호출로 사전 데이터를 생성하면 시나리오가 자기 완결적이 된다.

### cleanup.sql (TRUNCATE 방식)
```sql
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE member;
TRUNCATE TABLE category;
SET REFERENTIAL_INTEGRITY TRUE;
```

### H2 컬럼명
- JPA 기본 네이밍 전략: camelCase → snake_case (예: `imageUrl` → `image_url`)

## 테스트 격리
- `RANDOM_PORT`에서 `@Transactional` 롤백은 **동작하지 않는다**. 별도 스레드에서 HTTP 요청을 처리하므로 테스트 트랜잭션과 분리됨.
- Cucumber `@Before` 훅에서 cleanup SQL을 실행하여 시나리오 간 격리를 보장한다.

## 에러 응답
- `GlobalExceptionHandler`가 `IllegalStateException`/`NoSuchElementException`을 **400 BAD_REQUEST**로 처리한다.
- 재고 부족, 존재하지 않는 엔티티 조회 시 400 응답을 기대한다.

## 실행 예시

사용자: "선물하기 시나리오 작성해줘"

→ 수행할 작업:
1. Cucumber 인프라 파일 존재 여부 확인 (없으면 생성)
2. `src/test/resources/features/gift.feature` 작성 (비즈니스 언어로 시나리오 정의)
3. `src/test/java/gift/cucumber/steps/GiftSteps.java` 작성 (Step Definition 구현)
4. `./gradlew test --tests "gift.cucumber.CucumberTest"` 실행하여 통과 확인
