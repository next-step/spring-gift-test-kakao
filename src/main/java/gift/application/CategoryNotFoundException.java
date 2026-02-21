package gift.application;

import gift.application.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException(Long givenCategoryId) {
        super(String.format(
                "%d 에 해당하는 카테고리를 찾을 수 없습니다.",
                givenCategoryId
        ));
    }
}
