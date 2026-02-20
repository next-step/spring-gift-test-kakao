package gift.support;

import gift.model.Member;
import gift.model.MemberRepository;
import org.springframework.stereotype.Component;

@Component
public class MemberFixture {

    private final MemberRepository memberRepository;

    public MemberFixture(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private String name = "테스트유저";
        private String email = "test@test.com";

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Member build() {
            return memberRepository.save(new Member(name, email));
        }
    }
}