package gift.application;

import gift.application.exception.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(Long givenProductId) {
        super(String.format(
                "%d 에 해당하는 상품을 찾을 수 없습니다.",
                givenProductId
        ));
    }
}
