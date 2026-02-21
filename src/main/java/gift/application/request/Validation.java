package gift.application.request;

import gift.application.exception.BadRequestException;
import gift.application.exception.GlobalExceptionControllerAdvice;

/**
 * 요청 validation 을 위한 계약
 */
public interface Validation {

    /**
     * 주어진 요청 객체가 유효한지 확인하는 메서드
     *
     * @throws BadRequestException 유효하지 않다면 throw
     * @see BadRequestException
     * @see GlobalExceptionControllerAdvice
     */
    void validate() throws BadRequestException;
}
