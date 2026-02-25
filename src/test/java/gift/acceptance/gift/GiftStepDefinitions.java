package gift.acceptance.gift;

import gift.acceptance.AcceptanceContext;
import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import gift.support.CategoryFixture;
import gift.support.MemberFixture;
import gift.support.OptionFixture;
import gift.support.ProductFixture;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GiftStepDefinitions {

    @Autowired
    private AcceptanceContext context;

    @Autowired
    private CategoryFixture categoryFixture;

    @Autowired
    private ProductFixture productFixture;

    @Autowired
    private OptionFixture optionFixture;

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @조건("재고가 {int}인 옵션이 등록되어 있다")
    public void 재고가_인_옵션이_등록되어_있다(int quantity) {
        Category category = categoryFixture.builder().name("음료").build();
        Product product = productFixture.builder().name("아메리카노").price(4500).category(category).build();
        Option option = optionFixture.builder().name("ICE").quantity(quantity).product(product).build();
        context.setOptionId(option.getId());
    }

    @그리고("보내는 회원과 받는 회원이 등록되어 있다")
    public void 보내는_회원과_받는_회원이_등록되어_있다() {
        Member sender = memberFixture.builder().name("보내는사람").email("sender@test.com").build();
        Member receiver = memberFixture.builder().name("받는사람").email("receiver@test.com").build();
        context.setSenderId(sender.getId());
        context.setReceiverId(receiver.getId());
    }

    @조건("보내는 회원이 등록되어 있다")
    public void 보내는_회원이_등록되어_있다() {
        Member sender = memberFixture.builder().name("보내는사람").email("sender@test.com").build();
        context.setSenderId(sender.getId());
    }

    @그리고("수량 {int}으로 선물이 전달되어 있다")
    public void 수량_으로_선물이_전달되어_있다(int quantity) {
        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", quantity,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(200);
    }

    @만약("수량 {int}으로 선물 전달을 요청하면")
    public void 수량_으로_선물_전달을_요청하면(int quantity) {
        var response = given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", quantity,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .extract();

        context.setResponse(response);
    }

    @만약("존재하지 않는 옵션으로 선물 전달을 요청하면")
    public void 존재하지_않는_옵션으로_선물_전달을_요청하면() {
        var response = given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", 999999,
                        "quantity", 1,
                        "receiverId", 999999,
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .extract();

        context.setResponse(response);
    }

    @그리고("옵션의 재고가 {int}이다")
    public void 옵션의_재고가_이다(int expectedQuantity) {
        Integer quantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM option WHERE id = ?",
                Integer.class,
                context.getOptionId()
        );
        assertThat(quantity, equalTo(expectedQuantity));
    }
}