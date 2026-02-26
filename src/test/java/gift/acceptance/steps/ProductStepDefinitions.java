package gift.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static gift.acceptance.steps.CommonStepDefinitions.*;
import static org.hamcrest.Matchers.equalTo;

public class ProductStepDefinitions {

    @Autowired
    private ProductApiClient api;

    @Given("{string} 카테고리에 {string} 상품이 가격 {int}원으로 등록되어 있다")
    public void 상품이_등록되어_있다(String categoryName, String productName, int price) {
        Long categoryId = categoryIds.get(categoryName);
        api.상품을_DB에_등록한다(productName, price, "http://test.com/img.jpg", categoryId);
    }

    @When("{string} 카테고리에 {string} 상품을 가격 {int}원으로 등록한다")
    public void 상품을_등록한다(String categoryName, String name, int price) {
        Long categoryId = categoryIds.get(categoryName);
        latestResponse = api.상품을_등록한다(name, price, "http://test.com/img.jpg", categoryId);
        latestStatusCode = latestResponse.statusCode();
    }

    @When("존재하지 않는 카테고리에 {string} 상품을 가격 {int}원으로 등록한다")
    public void 존재하지_않는_카테고리에_등록한다(String name, int price) {
        latestResponse = api.상품을_등록한다(name, price, "http://test.com/img.jpg", 999L);
        latestStatusCode = latestResponse.statusCode();
    }

    @When("{string} 카테고리에 이름 없이 상품을 등록한다")
    public void 이름_없이_상품을_등록한다(String categoryName) {
        Long categoryId = categoryIds.get(categoryName);
        latestResponse = api.상품을_이름없이_등록한다(1000, "http://test.com/img.jpg", categoryId);
        latestStatusCode = latestResponse.statusCode();
    }

    @When("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        latestResponse = api.상품_목록을_조회한다();
        latestStatusCode = latestResponse.statusCode();
    }

    @Then("{string} 상품의 가격이 {int}원이다")
    public void 상품의_가격이_원이다(String name, int price) {
        latestResponse.then()
            .body("find { it.name == '" + name + "' }.price", equalTo(price));
    }
}
