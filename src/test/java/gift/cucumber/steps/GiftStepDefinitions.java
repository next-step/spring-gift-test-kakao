package gift.cucumber.steps;

import gift.model.*;
import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long senderId;
    private Long receiverId;
    private Long currentOptionId;
    private Response lastResponse;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        jdbcTemplate.execute(
                "TRUNCATE TABLE wish, option, product, category, member RESTART IDENTITY CASCADE"
        );
    }

    @조건("회원 {string}이 존재한다")
    public void 회원이_존재한다(String name) {
        Member member = memberRepository.save(new Member(name, name + "@test.com"));
        if ("보내는사람".equals(name)) {
            senderId = member.getId();
        } else {
            receiverId = member.getId();
        }
    }

    @조건("재고가 {int}인 옵션이 존재한다")
    public void 재고가_n인_옵션이_존재한다(int stock) {
        Category category = categoryRepository.save(new Category("테스트 카테고리"));
        Product product = productRepository.save(
                new Product("테스트 상품", 10000, "http://test.jpg", category));
        Option option = optionRepository.save(new Option("테스트 옵션", stock, product));
        currentOptionId = option.getId();
    }

    @만일("{int}개를 선물한다")
    public void n개를_선물한다(int quantity) {
        lastResponse = given()
                .contentType("application/json")
                .header("Member-Id", String.valueOf(senderId))
                .body("""
                        {
                            "optionId": %d,
                            "quantity": %d,
                            "receiverId": %d,
                            "message": "선물입니다"
                        }
                        """.formatted(currentOptionId, quantity, receiverId))
                .when()
                .post("/api/gifts");
    }

    @만일("옵션ID {long}로 {int}개를 선물한다")
    public void 옵션ID로_n개를_선물한다(long optionId, int quantity) {
        lastResponse = given()
                .contentType("application/json")
                .header("Member-Id", String.valueOf(senderId))
                .body("""
                        {
                            "optionId": %d,
                            "quantity": %d,
                            "receiverId": %d,
                            "message": "선물입니다"
                        }
                        """.formatted(optionId, quantity, receiverId))
                .when()
                .post("/api/gifts");
    }

    @그러면("응답 상태코드는 {int}이다")
    public void 응답_상태코드는_n이다(int statusCode) {
        assertThat(lastResponse.statusCode()).isEqualTo(statusCode);
    }

    @그러면("재고는 {int}이다")
    public void 재고는_n이다(int expectedStock) {
        // 앱 컨테이너(별도 JVM)가 변경한 DB 상태를 직접 조회
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT quantity FROM option WHERE id = ?", Integer.class, currentOptionId);
        assertThat(actual).isEqualTo(expectedStock);
    }
}
