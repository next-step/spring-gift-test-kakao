package gift.acceptance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleanup {

    private final EntityManager entityManager;
    private final List<String> tableNames;

    public DatabaseCleanup(final EntityManager entityManager) {
        this.entityManager = entityManager;
        this.tableNames = extractTableNames();
    }

    @Transactional
    public void execute() {
        entityManager.flush();
        String joined = String.join(", ", tableNames);
        entityManager.createNativeQuery("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE")
                .executeUpdate();
    }

    private List<String> extractTableNames() {
        return entityManager.getMetamodel().getEntities().stream()
                .map(this::resolveTableName)
                .toList();
    }

    private String resolveTableName(final EntityType<?> entity) {
        Table table = entity.getJavaType().getAnnotation(Table.class);
        if (table != null && !table.name().isBlank()) {
            return table.name();
        }
        return camelToSnake(entity.getName());
    }

    private String camelToSnake(final String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
