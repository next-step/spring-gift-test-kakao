package gift.support;

import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import org.springframework.stereotype.Component;

@Component
public class OptionFixture {

    private final OptionRepository optionRepository;

    public OptionFixture(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private String name = "기본옵션";
        private int quantity = 10;
        private Product product;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder product(Product product) {
            this.product = product;
            return this;
        }

        public Option build() {
            return optionRepository.save(new Option(name, quantity, product));
        }
    }
}