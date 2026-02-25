package gift;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("cucumber")
public class PostgresDatabaseCleaner implements DatabaseCleaner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void clear() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE wish, option, product, category, member RESTART IDENTITY CASCADE"
        );
    }
}
