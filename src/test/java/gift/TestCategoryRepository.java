package gift;

import gift.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCategoryRepository extends JpaRepository<Category, Long> {

}
