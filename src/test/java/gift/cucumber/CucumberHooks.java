package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.SQLException;
import java.util.Objects;

public class CucumberHooks {

    private final JdbcTemplate jdbcTemplate;

    public CucumberHooks(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() throws SQLException {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;

        ScriptUtils.executeSqlScript(
                Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection(),
                new ClassPathResource("/sql/cleanup-pg.sql")
        );
    }
}
