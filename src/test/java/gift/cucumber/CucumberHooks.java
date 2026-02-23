package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CucumberHooks {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        // 컨테이너 App으로 HTTP 요청 (Host → localhost:28080 → Docker App)
        RestAssured.baseURI = "http://localhost:28080";

        // DB 초기화 (Host → localhost:5432 → Docker PostgreSQL)
        jdbcTemplate.execute("TRUNCATE TABLE option, wish, product, category, member CASCADE");
    }
}
