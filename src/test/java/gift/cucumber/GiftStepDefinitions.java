package gift.cucumber;

import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @먼저("보내는 회원과 받는 회원이 존재한다")
    public void 보내는_회원과_받는_회원이_존재한다() {
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        context.setSenderId(sender.getId());
        context.setReceiverId(receiver.getId());
    }

    @먼저("재고가 {int}개인 옵션이 존재한다")
    public void 재고가_n개인_옵션이_존재한다(int quantity) {
        var categoryResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "음료"))
                .when()
                .post("/api/categories")
                .then()
                .extract();
        assertThat(categoryResponse.statusCode()).isEqualTo(200);

        var productResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "http://example.com/image.jpg",
                        "categoryId", categoryResponse.jsonPath().getLong("id")
                ))
                .when()
                .post("/api/products")
                .then()
                .extract();
        assertThat(productResponse.statusCode()).isEqualTo(200);

        var product = productRepository.findById(productResponse.jsonPath().getLong("id")).get();
        var option = optionRepository.save(new Option("ICE", quantity, product));
        context.setOptionId(option.getId());
    }

    @먼저("{string} 상품에 재고가 {int}개인 {string} 옵션이 존재한다")
    public void 상품에_옵션이_존재한다(String productName, int quantity, String optionName) {
        var product = productRepository.findById(context.getProductId(productName)).get();
        var option = optionRepository.save(new Option(optionName, quantity, product));
        context.setOptionId(option.getId());
    }

    @먼저("해당 옵션을 {int}개 선물에 성공한다")
    public void 해당_옵션을_선물에_성공한다(int quantity) {
        var response = RestAssured.given()
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
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @만일("해당 옵션을 {int}개 선물하면")
    public void 해당_옵션을_선물하면(int quantity) {
        var response = RestAssured.given()
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

    @만일("존재하지 않는 옵션을 선물하면")
    public void 존재하지_않는_옵션을_선물하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", Long.MAX_VALUE,
                        "quantity", 1,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("존재하지 않는 회원이 해당 옵션을 선물하면")
    public void 존재하지_않는_회원이_선물하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", Long.MAX_VALUE)
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", 1,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("인증 없이 해당 옵션을 선물하면")
    public void 인증_없이_선물하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", 1,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then()
                .extract();
        context.setResponse(response);
    }

    @그러면("선물에 성공한다")
    public void 선물에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
    }

    @그러면("선물에 실패한다")
    public void 선물에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("해당 옵션의 재고는 {int}개이다")
    public void 해당_옵션의_재고는_n개이다(int expectedQuantity) {
        var option = optionRepository.findById(context.getOptionId()).get();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
