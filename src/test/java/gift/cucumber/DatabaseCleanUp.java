package gift.cucumber;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseCleanUp {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Before(order = 0)
    public void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, member, category CASCADE");
        jdbcTemplate.execute("ALTER SEQUENCE category_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE product_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE option_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE wish_id_seq RESTART WITH 1");
    }
}
