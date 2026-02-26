package gift.container;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

public class ContainerHooks {

    @Autowired
    private DataSource dataSource;

    @Value("${test.sql.dialect:h2}")
    private String sqlDialect;

    @Before
    public void setUp() throws Exception {
        RestAssured.port = 28080;
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/" + sqlDialect + "/cleanup.sql"));
        }
    }
}
