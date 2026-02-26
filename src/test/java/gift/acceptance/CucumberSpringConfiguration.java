package gift.acceptance;

import gift.acceptance.steps.CommonStepDefinitions;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member CASCADE");
        CommonStepDefinitions.reset();
    }
}
