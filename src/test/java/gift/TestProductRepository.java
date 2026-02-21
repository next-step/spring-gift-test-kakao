package gift;

import gift.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestProductRepository extends JpaRepository<Product, Long> {

}
