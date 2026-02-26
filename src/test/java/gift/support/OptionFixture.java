package gift.support;

import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class OptionFixture {

    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionFixture(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private String name = "기본옵션";
        private int quantity = 10;
        private Long productId;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Option build() {
            Product product = productRepository.findById(productId).orElseThrow();
            return optionRepository.save(new Option(name, quantity, product));
        }
    }
}