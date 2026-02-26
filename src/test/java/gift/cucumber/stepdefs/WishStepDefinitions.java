package gift.cucumber.stepdefs;

import gift.cucumber.ScenarioContext;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import gift.model.Wish;
import gift.model.WishRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WishStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DataSource dataSource;

    @Value("${test.sql.dialect:h2}")
    private String sqlDialect;

    @Given("상품 {string}이 존재한다")
    public void 상품이_존재한다(String productName) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-data.sql"));
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/" + sqlDialect + "/reset-sequences.sql"));
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/wish/success.sql"));
        }
    }

    @When("{string}이 {string}을 위시리스트에 추가한다")
    public void 회원이_상품을_위시리스트에_추가한다(String memberName, String productName) {
        Member member = memberRepository.findAll().stream()
            .filter(m -> m.getName().equals(memberName))
            .findFirst()
            .orElseThrow();
        Product product = productRepository.findAll().stream()
            .filter(p -> p.getName().equals(productName))
            .findFirst()
            .orElseThrow();

        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", member.getId())
                .body("""
                    {"productId": %d}
                    """.formatted(product.getId()))
            .when()
                .post("/api/wishes")
        );
    }

    @When("{string}이 존재하지 않는 상품을 위시리스트에 추가한다")
    public void 회원이_존재하지_않는_상품을_위시리스트에_추가한다(String memberName) {
        Member member = memberRepository.findAll().stream()
            .filter(m -> m.getName().equals(memberName))
            .findFirst()
            .orElseThrow();

        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", member.getId())
                .body("""
                    {"productId": 999}
                    """)
            .when()
                .post("/api/wishes")
        );
    }

    @When("존재하지 않는 회원이 {string}을 위시리스트에 추가한다")
    public void 존재하지_않는_회원이_상품을_위시리스트에_추가한다(String productName) {
        Product product = productRepository.findAll().stream()
            .filter(p -> p.getName().equals(productName))
            .findFirst()
            .orElseThrow();

        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 999L)
                .body("""
                    {"productId": %d}
                    """.formatted(product.getId()))
            .when()
                .post("/api/wishes")
        );
    }

    @When("상품 ID에 {string}을 담아 위시리스트 요청을 보낸다")
    public void 상품_ID에_잘못된값을_담아_위시리스트_요청을_보낸다(String invalidValue) {
        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 1L)
                .body("""
                    {"productId": "%s"}
                    """.formatted(invalidValue))
            .when()
                .post("/api/wishes")
        );
    }

    @And("위시리스트에 {int}개 항목이 저장되어 있다")
    public void 위시리스트에_N개_항목이_저장되어_있다(int expectedCount) {
        List<Wish> wishes = wishRepository.findAll();
        assertThat(wishes).hasSize(expectedCount);
    }

    @And("{string}의 위시리스트에 {string}이 있다")
    public void 회원의_위시리스트에_상품이_있다(String memberName, String productName) {
        List<Wish> wishes = wishRepository.findAll();
        assertThat(wishes)
            .anyMatch(w -> w.getMember().getName().equals(memberName) && w.getProduct().getName().equals(productName));
    }
}
