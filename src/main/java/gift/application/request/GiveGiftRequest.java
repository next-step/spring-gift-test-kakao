package gift.application.request;

import gift.application.exception.BadRequestException;

public record GiveGiftRequest(
        Long optionId,
        int quantity,
        Long receiverId,
        String message
) implements Validation {

    @Override
    public void validate() throws BadRequestException {

        if (optionId == null) {
            throw new BadRequestException("옵션 ID 는 필수입니다.");
        }

        if (quantity < 0) {
            throw new BadRequestException("수량은 0 보다 작을 수 없습니다.");
        }

        if (receiverId == null) {
            throw new BadRequestException("선물 수신자 ID 는 필수입니다.");
        }
    }
}
