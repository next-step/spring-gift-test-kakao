package gift.error;

public class CommonException extends BusinessException {

    public CommonException(final CommonErrorCode errorCode) {
        super(errorCode);
    }
}
