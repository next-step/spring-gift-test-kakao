package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

public class CucumberHooks {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        RestAssured.port = port;

        jdbcTemplate.execute("TRUNCATE TABLE option, wish, product, category, member CASCADE");
    }
}
