package gift.application;

import gift.application.exception.NotFoundException;

public class MemberNotFoundException extends NotFoundException {

    public MemberNotFoundException(Long givenMemberId) {
        super(String.format(
                "%d 에 해당하는 멤버를 찾을 수 없습니다.",
                givenMemberId
        ));
    }
}
