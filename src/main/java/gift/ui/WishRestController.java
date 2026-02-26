package gift.ui;

import gift.application.CreateWishRequest;
import gift.application.WishService;
import gift.model.Wish;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishes")
public class WishRestController {
    private final WishService wishService;

    public WishRestController(final WishService wishService) {
        this.wishService = wishService;
    }

    @PostMapping
    public Wish create(@RequestBody CreateWishRequest request, @RequestHeader("Member-Id") Long memberId) {
        return wishService.create(memberId, request);
    }
}
