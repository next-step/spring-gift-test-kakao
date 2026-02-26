package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

public class Hooks {

    @LocalServerPort
    int port;

    @Autowired
    private DataSource dataSource;

    @Value("${test.sql.dialect:h2}")
    private String sqlDialect;

    @Before
    public void setUp() throws Exception {
        RestAssured.port = port;
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/" + sqlDialect + "/cleanup.sql"));
        }
    }
}
