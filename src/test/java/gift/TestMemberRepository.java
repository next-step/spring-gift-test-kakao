package gift;

import gift.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestMemberRepository extends JpaRepository<Member, Long> {

}
