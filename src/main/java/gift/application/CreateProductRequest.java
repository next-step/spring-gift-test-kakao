package gift.application;

public record CreateProductRequest(String name, int price, String imageUrl, Long categoryId) {
}
