package gift.ui;

import gift.application.GiftService;
import gift.application.request.GiveGiftRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gifts")
public class GiftRestController {

    private final GiftService giftService;

    public GiftRestController(final GiftService giftService) {
        this.giftService = giftService;
    }

    @PostMapping
    public void give(
            @RequestBody GiveGiftRequest request,
            @RequestHeader("Member-Id") Long memberId
    ) {
        request.validate();

        giftService.give(request, memberId);
    }
}
