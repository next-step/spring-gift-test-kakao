package gift.acceptance.steps;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class GiftApiClient {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Long 회원을_등록한다(String name, String email) {
        jdbcTemplate.update("INSERT INTO member (name, email) VALUES (?, ?)", name, email);
        return jdbcTemplate.queryForObject(
            "SELECT id FROM member WHERE name = ?", Long.class, name);
    }

    public int 옵션_재고를_조회한다(Long optionId) {
        return jdbcTemplate.queryForObject(
            "SELECT quantity FROM option WHERE id = ?", Integer.class, optionId);
    }

    public Long 옵션을_등록한다(String name, int quantity, Long productId) {
        jdbcTemplate.update(
            "INSERT INTO option (name, quantity, product_id) VALUES (?, ?, ?)",
            name, quantity, productId);
        return jdbcTemplate.queryForObject(
            "SELECT id FROM option WHERE name = ? AND product_id = ?",
            Long.class, name, productId);
    }

    public Response 선물을_보낸다(Long senderId, Long optionId, int quantity, Long receiverId) {
        return given()
            .contentType(ContentType.JSON)
            .header("Member-Id", senderId)
            .body(Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "receiverId", receiverId,
                "message", "선물입니다"
            ))
        .when()
            .post("/api/gifts");
    }
}
