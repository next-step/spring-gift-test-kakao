package gift.application.request;

import gift.application.exception.BadRequestException;

public record CreateOptionRequest(
        String name,
        int quantity,
        Long productId
) implements Validation {

    @Override
    public void validate() throws BadRequestException {

        if (quantity < 0) {
            throw new BadRequestException("수량은 0 보다 작을 수 없습니다.");
        }

        if (productId == null) {
            throw new BadRequestException("상품 ID 는 필수입니다.");
        }
    }
}
