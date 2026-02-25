package gift.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @jakarta.annotation.PostConstruct
    void init() {
        tableNames = entityManager.getMetamodel().getEntities().stream()
                .map(EntityType::getName)
                .map(this::toSnakeCase)
                .toList();
    }

    @Transactional
    public void execute() {
        entityManager.flush();
        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE \"" + tableName + "\" CASCADE").executeUpdate();
        }
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
