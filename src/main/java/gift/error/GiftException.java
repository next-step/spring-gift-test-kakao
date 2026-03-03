package gift.error;

public class GiftException extends BusinessException {

    public GiftException(final GiftErrorCode errorCode) {
        super(errorCode);
    }
}
