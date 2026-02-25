package gift.acceptance;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseCleanup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        RestAssured.port = 28080;
    }

    @Before(order = 0)
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member RESTART IDENTITY CASCADE");
    }
}
