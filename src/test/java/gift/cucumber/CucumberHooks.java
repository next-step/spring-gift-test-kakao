package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.SQLException;
import java.util.Objects;

public class CucumberHooks {

    @LocalServerPort
    private int port;

    private final JdbcTemplate jdbcTemplate;

    public CucumberHooks(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() throws SQLException {
        RestAssured.port = port;

        ScriptUtils.executeSqlScript(
                Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection(),
                new ClassPathResource("/sql/cleanup.sql")
        );
    }
}
