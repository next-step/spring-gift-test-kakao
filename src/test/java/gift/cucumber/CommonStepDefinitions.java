package gift.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonStepDefinitions {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SharedContext sharedContext;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        jdbcTemplate.execute("TRUNCATE TABLE option, product, category, wish, member RESTART IDENTITY CASCADE");
    }

    @그러면("응답 상태코드는 {int}이다")
    public void 응답_상태코드는_이다(int statusCode) {
        Response response = sharedContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(statusCode);
    }
}
