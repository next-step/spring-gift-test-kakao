package gift.cucumber;

import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.RestAssured;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    private final JdbcTemplate jdbcTemplate;

    public CucumberSpringConfiguration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        RestAssured.port = port;
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member CASCADE");
    }
}
