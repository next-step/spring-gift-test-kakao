package gift.model;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(int requested, int available) {
        super("재고가 부족합니다. 요청: " + requested + ", 현재 재고: " + available);
    }
}
