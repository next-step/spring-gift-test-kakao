package gift.e2e;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class E2eHooks {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        RestAssured.port = 28080;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member CASCADE");
    }
}
