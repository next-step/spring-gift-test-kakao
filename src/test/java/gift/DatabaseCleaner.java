package gift;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleaner {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void clear() {
		entityManager.flush();
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
		for (final String tableName : getTableNames()) {
			entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
		}
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
	}

	private List<String> getTableNames() {
		return entityManager.getMetamodel().getEntities().stream()
			.map(EntityType::getName)
			.toList();
	}
}
