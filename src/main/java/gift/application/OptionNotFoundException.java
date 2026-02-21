package gift.application;

import gift.application.exception.NotFoundException;

public class OptionNotFoundException extends NotFoundException {

    public OptionNotFoundException(Long givenOptionId) {
        super(String.format(
                "%d 에 해당하는 옵션을 찾을 수 없습니다.",
                givenOptionId
        ));
    }
}
