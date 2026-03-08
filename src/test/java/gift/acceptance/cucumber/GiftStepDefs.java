package gift.acceptance.cucumber;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefs {

    @Autowired
    SharedContext context;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @만일("회원 {int}번이 옵션 {int}번 {int}개를 회원 {int}번에게 선물하면")
    public void 선물하면(int senderId, int optionId, int quantity, int receiverId) {
        var response = given()
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
        context.setResponse(response);
    }

    @그리고("옵션 {int}번의 재고는 {int}이다")
    public void 옵션의_재고는_이다(int optionId, int expectedQuantity) {
        Integer stock = jdbcTemplate.queryForObject(
            "SELECT quantity FROM option WHERE id = ?", Integer.class, optionId);
        assertThat(stock).isEqualTo(expectedQuantity);
    }
}
