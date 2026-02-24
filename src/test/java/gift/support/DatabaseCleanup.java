package gift.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    private List<String> tableNames;
    private boolean isPostgres;

    public DatabaseCleanup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @jakarta.annotation.PostConstruct
    void init() throws SQLException {
        String dbProductName = dataSource.getConnection().getMetaData().getDatabaseProductName();
        isPostgres = dbProductName.equalsIgnoreCase("PostgreSQL");

        tableNames = entityManager.getMetamodel().getEntities().stream()
                .map(EntityType::getName)
                .map(this::toSnakeCase)
                .toList();
    }

    @Transactional
    public void execute() {
        entityManager.flush();
        if (isPostgres) {
            for (String tableName : tableNames) {
                entityManager.createNativeQuery("TRUNCATE TABLE \"" + tableName + "\" CASCADE").executeUpdate();
            }
        } else {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            for (String tableName : tableNames) {
                entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        }
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
