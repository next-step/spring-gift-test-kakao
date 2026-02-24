package gift.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    private final boolean isPostgres;
    private List<String> tableNames;

    public DatabaseCleanup(Environment environment) {
        this.isPostgres = Arrays.asList(environment.getActiveProfiles()).contains("cucumber");
    }

    @jakarta.annotation.PostConstruct
    void init() {
        if (isPostgres) {
            tableNames = entityManager.createNativeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'"
            ).getResultList();
        } else {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .map(EntityType::getName)
                    .toList();
        }
    }

    @Transactional
    public void execute() {
        entityManager.flush();
        if (isPostgres) {
            for (String tableName : tableNames) {
                entityManager.createNativeQuery("TRUNCATE TABLE \"" + tableName + "\" CASCADE")
                        .executeUpdate();
            }
        } else {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            for (String tableName : tableNames) {
                entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        }
    }
}