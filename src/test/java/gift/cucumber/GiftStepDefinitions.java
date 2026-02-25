package gift.cucumber;

import gift.model.Option;
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
    private SharedState sharedState;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OptionRepository optionRepository;

    @그리고("이메일 {string}인 {string} 회원이 등록되어 있다")
    public void 이메일_인_회원이_등록되어_있다(String email, String name) {
        jdbcTemplate.update("INSERT INTO member (name, email) VALUES (?, ?)", name, email);
    }

    @조건("{string} 상품에 재고 {int}개인 {string} 옵션이 등록되어 있다")
    public void 상품에_재고_개인_옵션이_등록되어_있다(String productName, int quantity, String optionName) {
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM product WHERE name = ?",
                Long.class,
                productName
        );
        jdbcTemplate.update(
                "INSERT INTO option (name, quantity, product_id) VALUES (?, ?, ?)",
                optionName, quantity, productId
        );
    }

    @만약("{string}이 {string}에게 {string} 옵션으로 {int}개를 선물하면")
    public void 이_에게_옵션으로_개를_선물하면(String senderName, String receiverName, String optionName, int quantity) {
        Long senderId = jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE name = ?",
                Long.class,
                senderName
        );
        Long receiverId = jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE name = ?",
                Long.class,
                receiverName
        );
        Long optionId = jdbcTemplate.queryForObject(
                "SELECT id FROM option WHERE name = ?",
                Long.class,
                optionName
        );

        sharedState.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts"));
    }

    @그리고("{string} 옵션의 재고는 {int}개이다")
    public void 옵션의_재고는_개이다(String optionName, int expectedQuantity) {
        Long optionId = jdbcTemplate.queryForObject(
                "SELECT id FROM option WHERE name = ?",
                Long.class,
                optionName
        );
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }

    @만약("{string}이 {string}에게 존재하지 않는 옵션으로 선물하면")
    public void 이_에게_존재하지_않는_옵션으로_선물하면(String senderName, String receiverName) {
        Long senderId = jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE name = ?",
                Long.class,
                senderName
        );
        Long receiverId = jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE name = ?",
                Long.class,
                receiverName
        );

        sharedState.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", 9999L,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts"));
    }
}
