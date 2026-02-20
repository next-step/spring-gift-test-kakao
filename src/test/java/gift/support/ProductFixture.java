package gift.support;

import gift.model.Category;
import gift.model.Product;
import gift.model.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class ProductFixture {

    private final ProductRepository productRepository;

    public ProductFixture(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private String name = "테스트상품";
        private int price = 1000;
        private String imageUrl = "https://example.com/image.png";
        private Category category;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder price(int price) {
            this.price = price;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Product build() {
            return productRepository.save(new Product(name, price, imageUrl, category));
        }
    }
}