package gift.ui;

import gift.application.CreateMemberRequest;
import gift.application.MemberService;
import gift.model.Member;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/members")
@RestController
public class MemberRestController {
    private final MemberService memberService;

    public MemberRestController(final MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public Member create(@RequestBody final CreateMemberRequest request) {
        return memberService.create(request);
    }

    @GetMapping
    public List<Member> retrieve() {
        return memberService.retrieve();
    }
}