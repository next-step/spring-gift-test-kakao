package gift.cucumber;

import gift.model.OptionRepository;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private SharedContext sharedContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OptionRepository optionRepository;

    @조건("회원 {string}과 회원 {string}이 존재한다")
    public void 회원과_회원이_존재한다(String senderName, String receiverName) {
        jdbcTemplate.update("INSERT INTO member (name, email) VALUES (?, ?)", senderName, senderName + "@test.com");
        Long senderId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE name = ?", Long.class, senderName);
        sharedContext.putMemberId(senderName, senderId);

        jdbcTemplate.update("INSERT INTO member (name, email) VALUES (?, ?)", receiverName, receiverName + "@test.com");
        Long receiverId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE name = ?", Long.class, receiverName);
        sharedContext.putMemberId(receiverName, receiverId);
    }

    @그리고("카테고리 {string}에 상품 {string}가 있고 수량이 {int}인 옵션 {string}가 존재한다")
    public void 카테고리에_상품이_있고_옵션이_존재한다(String categoryName, String productName, int quantity, String optionName) {
        jdbcTemplate.update("INSERT INTO category (name) VALUES (?)", categoryName);
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM category WHERE name = ?", Long.class, categoryName);

        jdbcTemplate.update("INSERT INTO product (name, price, image_url, category_id) VALUES (?, 10000, 'http://image.url', ?)", productName, categoryId);
        Long productId = jdbcTemplate.queryForObject("SELECT id FROM product WHERE name = ?", Long.class, productName);

        jdbcTemplate.update("INSERT INTO option (name, quantity, product_id) VALUES (?, ?, ?)", optionName, quantity, productId);
        Long optionId = jdbcTemplate.queryForObject("SELECT id FROM option WHERE name = ?", Long.class, optionName);
        sharedContext.putOptionId(optionName, optionId);
    }

    @만약("{string}이 {string} {int}개를 {string}에게 선물한다")
    public void 회원이_옵션을_회원에게_선물한다(String senderName, String optionName, int quantity, String receiverName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sharedContext.getMemberId(senderName))
                .body(Map.of(
                        "optionId", sharedContext.getOptionId(optionName),
                        "quantity", quantity,
                        "receiverId", sharedContext.getMemberId(receiverName),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");
        sharedContext.setResponse(response);
    }

    @만약("존재하지 않는 회원이 {string} {int}개를 {string}에게 선물한다")
    public void 존재하지_않는_회원이_선물한다(String optionName, int quantity, String receiverName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 9999L)
                .body(Map.of(
                        "optionId", sharedContext.getOptionId(optionName),
                        "quantity", quantity,
                        "receiverId", sharedContext.getMemberId(receiverName),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");
        sharedContext.setResponse(response);
    }

    @만약("{string}이 존재하지 않는 옵션 {int}개를 {string}에게 선물한다")
    public void 존재하지_않는_옵션을_선물한다(String senderName, int quantity, String receiverName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sharedContext.getMemberId(senderName))
                .body(Map.of(
                        "optionId", 9999L,
                        "quantity", quantity,
                        "receiverId", sharedContext.getMemberId(receiverName),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");
        sharedContext.setResponse(response);
    }

    @그리고("{string}의 재고는 {int}이다")
    public void 옵션의_재고는_이다(String optionName, int expectedQuantity) {
        Long optionId = sharedContext.getOptionId(optionName);
        var option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
