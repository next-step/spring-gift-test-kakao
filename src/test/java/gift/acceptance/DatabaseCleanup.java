package gift.acceptance;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseCleanup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before(order = 0)
    public void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member RESTART IDENTITY CASCADE");
    }
}
