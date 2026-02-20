 # 🚀 선물하기 서비스 테스트 작성 가이드

  본 문서는 **BDD 도구(Cucumber, Karate 등) 없이** `RestAssured`와 `JUnit5`를 활용하여 선물하기 서비스의 행동 기반 테스트를 작성하는 방법과 규칙을 정의합니다.

  ## 📌 테스트 핵심 원칙

    1. **Behavior-Driven (RestAssured):** 사용자의 행동(Given/When/Then)을 API 경계에서 검증합니다.
    2. **State Change Validation:** 단순히 성공 케이스만 확인하는 것이 아니라, **실패 시나리오**를 통해 상태 변화(재고 부족 등)가 정확히 일어나는지 증명합니다.
    3. **Data Management:** 테스트 데이터는 `SQL 스크립트(seed.sql)`를 통해 일관된 상태를 유지하며 관리합니다.
    4. **No BDD Tools:** Gherkin 문법 대신 Java 코드 내 주석과 메서드 체이닝으로 행동을 묘사합니다.

    ---

  ## 💾 데이터 셋 관리 (SQL Seed)

  테스트 실행 전, 고정된 기초 데이터를 DB에 삽입하여 테스트의 재현성을 보장합니다.

  `src/test/resources/data/seed.sql`

    ```sql
    - 기본 카테고리 및 상품 세팅INSERT INTO category (id, name) VALUES (1, '전자기기');
    INSERT INTO product (id, name, category_id) VALUES (1, '맥북 에어', 1);
    - 테스트용 옵션 세팅 (재고 1개)INSERT INTO product_option (id, product_id, name, quantity) VALUES (1, 1, 'M2 Silver', 1);
    - 테스트용 멤버 세팅INSERT INTO member (id, name, email) VALUES (1, '보내는이', 'sender@kakao.com');
    INSERT INTO member (id, name, email) VALUES (2, '받는이', 'receiver@kakao.com');
    ```
    
  ---

  ## 🛠️ 테스트 코드 작성 계획

  ### 1. 테스트 계층 구조

    - **Domain Unit Test:** `Option.decrease()`와 같은 핵심 로직 검증 (의존성 없음)
    - **API Behavior Test:** `RestAssured`를 사용한 종단 간(E2E) 흐름 검증

  ### 2. 핵심 시나리오 예시: 선물하기와 재고 변화

  가장 중요한 비즈니스 로직인 "선물 발송 시 재고 차감 및 부족 시 실패" 시나리오를 API 경계에서 테스트합니다.

    ```java
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Sql(scripts = "/data/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public class GiftAcceptanceTest {
    
        @LocalServerPort
        private int port;
    
        @BeforeEach
        void setUp() {
            RestAssured.port = port;
        }
    
        @Test
        @DisplayName("선물을 보내면 옵션 재고가 감소하며, 재고 부족 시 요청이 실패한다.")
        void 선물하기_재고_검증_시나리오() {
            // Given: id 1인 옵션의 재고가 1개인 상태 (seed.sql 기반)
            var giftRequest = new GiftRequest(1L, 1L, 2L, "생일 축하해!");
    
            // When: 첫 번째 선물 보내기
            RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(giftRequest)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .statusCode(201); // Then: 성공
    
            // When: 동일한 옵션으로 다시 선물 보내기
            RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(giftRequest)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .statusCode(400); // Then: 재고 부족으로 실패 (상태 변화 증명)
        }
    }
    ```
    
  ---

  ## 📈 단계별 테스트 작성 로드맵

  | **순서** | **대상** | **테스트 유형** | **검증 포인트** |
      | --- | --- | --- | --- |
  | **1** | `Option.decrease()` | Unit | 수량 차감 로직 및 `IllegalStateException` 발생 여부 |
  | **2** | `Category/Product` | API | 기본 CRUD 및 카테고리-상품 간의 연관 관계 저장 |
  | **3** | `OptionService` | Service | 상품 존재 여부 확인 후 옵션 생성 로직 |
  | **4** | `WishService` | Service | 특정 멤버의 위시리스트 담기 기능 |
  | **5** | `GiftService` | Integration | `FakeGiftDelivery`를 활용한 외부 연동 및 전체 트랜잭션 |
  | **6** | `GiftRestController` | Acceptance | **최종 행동 기반 테스트** (RestAssured 활용) |
    
  ---

  ## 💡 참고 사항

    - **실패 시나리오의 중요성:** `400 Bad Request` 또는 특정 비즈니스 예외 코드를 확인하여 시스템의 견고함을 증명하세요.
    - **RestAssured 활용:** API 응답의 `body`뿐만 아니라 `statusCode`, `header` 등을 종합적으로 검증합니다.
    - **Idempotency:** `@Sql` 설정을 통해 각 테스트 메서드 실행 전 DB를 초기화하여 테스트 간 간섭을 방지합니다.
