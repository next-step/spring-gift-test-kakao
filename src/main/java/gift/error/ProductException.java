package gift.error;

public class ProductException extends BusinessException {

    public ProductException(final ProductErrorCode errorCode) {
        super(errorCode);
    }
}
