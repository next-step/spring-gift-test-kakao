package gift.steps;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.equalTo;

public class CommonStepDefs {

    @Autowired
    private ScenarioState state;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Given("{string} 카테고리가 등록되어 있다")
    public void categoryExists(final String name) {
        final Category category = categoryRepository.save(new Category(name));
        state.put("category:" + name, category);
    }

    @Given("{string} 카테고리에 이름이 {string}이고 가격이 {int}원이고 이미지가 {string}인 상품이 등록되어 있다")
    public void productExists(final String categoryName, final String productName, final int price, final String imageUrl) {
        final Category category = state.get("category:" + categoryName);
        final Product product = productRepository.save(new Product(productName, price, imageUrl, category));
        state.put("product:" + productName, product);
    }

    @Given("{string} 상품에 이름이 {string}이고 재고가 {int}개인 옵션이 등록되어 있다")
    public void optionExists(final String productName, final String optionName, final int quantity) {
        final Product product = state.get("product:" + productName);
        final Option option = optionRepository.save(new Option(optionName, quantity, product));
        state.put("option:" + optionName, option);
    }

    @Given("이름이 {string}이고 이메일이 {string}인 회원이 등록되어 있다")
    public void memberExists(final String name, final String email) {
        final Member member = memberRepository.save(new Member(name, email));
        state.put("member:" + name, member);
    }

    @Then("응답 상태 코드는 {int}이다")
    public void responseStatusCodeIs(final int statusCode) {
        state.getResponse().then().statusCode(statusCode);
    }

    @Then("응답 본문의 {string} 필드는 {string}이다")
    public void responseFieldEquals(final String field, final String expected) {
        state.getResponse().then().body(field, equalTo(expected));
    }
}
