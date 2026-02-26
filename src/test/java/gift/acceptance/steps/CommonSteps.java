package gift.acceptance.steps;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CommonSteps {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void cleanUp() {
        jdbcTemplate.execute("TRUNCATE wish, option, product, category, member CASCADE");
    }
}
