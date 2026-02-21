package gift;

import gift.model.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestWishRepository extends JpaRepository<Wish, Long> {

}
