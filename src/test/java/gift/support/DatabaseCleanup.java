package gift.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;
    private final boolean isPostgres;
    private List<String> tableNames;

    public DatabaseCleanup(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.isPostgres = Arrays.asList(environment.getActiveProfiles()).contains("cucumber");
    }

    @jakarta.annotation.PostConstruct
    void init() {
        if (isPostgres) {
            tableNames = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                    String.class
            );
        } else {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .map(EntityType::getName)
                    .toList();
        }
    }

    public void execute() {
        if (isPostgres) {
            for (String tableName : tableNames) {
                jdbcTemplate.execute("TRUNCATE TABLE \"" + tableName + "\" CASCADE");
            }
        } else {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
            for (String tableName : tableNames) {
                jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
            }
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}