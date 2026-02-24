package gift.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {
	private final JdbcTemplate jdbcTemplate;

	public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void clean() {
		jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
		jdbcTemplate.execute("TRUNCATE TABLE wish");
		jdbcTemplate.execute("TRUNCATE TABLE option");
		jdbcTemplate.execute("TRUNCATE TABLE product");
		jdbcTemplate.execute("TRUNCATE TABLE category");
		jdbcTemplate.execute("TRUNCATE TABLE member");
		jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
	}
}
