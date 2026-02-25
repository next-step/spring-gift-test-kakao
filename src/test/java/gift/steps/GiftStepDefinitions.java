package gift.steps;

import gift.application.GiveGiftRequest;
import gift.fixture.GiftFixture;
import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Value("${app.port}")
    int appPort;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    private GiftFixture fixture;
    private Response response;

    @Before
    public void setUp() {
        RestAssured.port = appPort;
    }

    @Given("재고가 {int}개인 옵션이 준비되어 있다")
    public void 재고가_N개인_옵션이_준비되어_있다(int quantity) {
        fixture = GiftFixture.builder(memberRepository, categoryRepository, productRepository, optionRepository)
                .option("", quantity)
                .build();
    }

    @When("{int}개를 선물하면")
    public void N개를_선물하면(int quantity) {
        GiveGiftRequest request = new GiveGiftRequest(fixture.optionId(), quantity, fixture.receiverId(), "선물");

        response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", fixture.senderId())
                .body(request)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract().response();
    }

    @When("존재하지 않는 옵션으로 {int}개를 선물하면")
    public void 존재하지_않는_옵션으로_N개를_선물하면(int quantity) {
        Long nonExistentOptionId = fixture.optionId() + 1;
        GiveGiftRequest request = new GiveGiftRequest(nonExistentOptionId, quantity, fixture.receiverId(), "선물");

        response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", fixture.senderId())
                .body(request)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract().response();
    }

    @When("존재하지 않는 발신자가 {int}개를 선물하면")
    public void 존재하지_않는_발신자가_N개를_선물하면(int quantity) {
        Long nonExistentMemberId = Math.max(fixture.senderId(), fixture.receiverId()) + 1;
        GiveGiftRequest request = new GiveGiftRequest(fixture.optionId(), quantity, fixture.receiverId(), "선물");

        response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", nonExistentMemberId)
                .body(request)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract().response();
    }

    @Then("선물하기가 성공한다")
    public void 선물하기가_성공한다() {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Then("선물하기에 실패한다")
    public void 선물하기에_실패한다() {
        assertThat(response.jsonPath().getString("error")).isNotNull();
    }

    @And("옵션 재고는 {int}개이다")
    public void 옵션_재고는_N개이다(int expectedQuantity) {
        Integer remainingQuantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM options WHERE id = ?", Integer.class, fixture.optionId());
        assertThat(remainingQuantity).isEqualTo(expectedQuantity);
    }
}
