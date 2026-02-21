package gift.application.request;

import gift.application.exception.BadRequestException;

public record CreateProductRequest(
        String name,
        int price,
        String imageUrl,
        Long categoryId
) implements Validation {

    @Override
    public void validate() throws BadRequestException {
        if (price < 0) {
            throw new BadRequestException("상품 가격은 0 보다 작을 수 없습니다.");
        }

        if (categoryId == null) {
            throw new BadRequestException("카테고리 ID 는 필수입니다.");
        }
    }
}
