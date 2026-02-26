package gift.cucumber.steps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

import gift.cucumber.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CommonSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        // 시나리오 시작 전 DB 정리
        cleanup();
    }

    @After
    public void cleanup() {
        // JDBC로 직접 DB 정리 (Docker 컨테이너 앱과 독립적)
        jdbcTemplate.execute("DELETE FROM option");
        jdbcTemplate.execute("DELETE FROM product");
        jdbcTemplate.execute("DELETE FROM wish");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM member");
    }

    // === 공통 Given Steps (API 호출로 데이터 생성) ===

    @Given("카테고리 {string}가 존재한다")
    public void 카테고리가_존재한다(String name) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + name + "\"}")
                .post("/api/categories");

        Long categoryId = response.jsonPath().getLong("id");
        context.setCategoryId(categoryId);
    }

    @Given("상품 {string}이 존재한다")
    public void 상품이_존재한다(String name) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + name + "\", \"price\": 1000, \"imageUrl\": \"https://test.com/image.jpg\", \"categoryId\": " + context.getCategoryId() + "}")
                .post("/api/products");

        Long productId = response.jsonPath().getLong("id");
        context.setProductId(productId);
    }

    @Given("보내는 사람 {string}이 존재한다")
    public void 보내는_사람이_존재한다(String name) {
        // 직접 DB에 삽입 (Member API가 없으므로)
        jdbcTemplate.update("INSERT INTO member (name, email) VALUES (?, ?)", name, name + "@test.com");
        Long senderId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE name = ?", Long.class, name);
        context.setSenderId(senderId);
    }

    @Given("받는 사람 {string}이 존재한다")
    public void 받는_사람이_존재한다(String name) {
        // 직접 DB에 삽입 (Member API가 없으므로)
        jdbcTemplate.update("INSERT INTO member (name, email) VALUES (?, ?)", name, name + "@test.com");
        Long receiverId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE name = ?", Long.class, name);
        context.setReceiverId(receiverId);
    }

    @Given("재고가 {int}개인 옵션 {string}이 존재한다")
    public void 옵션이_존재한다(int quantity, String name) {
        // 직접 DB에 삽입 (Option API가 없으므로)
        jdbcTemplate.update("INSERT INTO option (name, quantity, product_id) VALUES (?, ?, ?)",
                name, quantity, context.getProductId());
        Long optionId = jdbcTemplate.queryForObject(
                "SELECT id FROM option WHERE name = ? AND product_id = ?",
                Long.class, name, context.getProductId());
        context.setOptionId(optionId);
    }

    // === 공통 Then Steps ===

    @Then("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드는(int statusCode) {
        context.getLastResponse().then().statusCode(statusCode);
    }

    @Then("목록에 1개 이상의 상품이 있다")
    public void 목록에_1개_이상의_상품이_있다() {
        context.getLastResponse().then().body("size()", greaterThanOrEqualTo(1));
    }

    @Then("목록에 1개 이상의 카테고리가 있다")
    public void 목록에_1개_이상의_카테고리가_있다() {
        context.getLastResponse().then().body("size()", greaterThanOrEqualTo(1));
    }

    @Then("목록에 {string}가 포함되어 있다")
    public void 목록에_이름이_포함되어_있다(String name) {
        context.getLastResponse().then().body("name", hasItem(name));
    }
}
