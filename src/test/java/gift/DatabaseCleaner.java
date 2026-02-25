package gift;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private static final List<String> TABLES = List.of(
            "wish", "option", "product", "category", "member"
    );

    public void clear() {
        if (isPostgresql()) {
            String tableList = String.join(", ", TABLES);
            jdbcTemplate.execute("TRUNCATE TABLE " + tableList + " CASCADE");
        } else {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
            TABLES.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private boolean isPostgresql() {
        try (var connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            return url.contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }
}
