package gift.application.request;

import gift.application.exception.BadRequestException;

public record CreateWishRequest(
        Long productId
) implements Validation {

    @Override
    public void validate() throws BadRequestException {
        if (productId == null) {
            throw new BadRequestException("상품 ID 는 필수입니다.");
        }
    }
}
