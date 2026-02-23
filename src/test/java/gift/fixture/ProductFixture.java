package gift.fixture;

import gift.model.Category;
import gift.model.CategoryRepository;

public class ProductFixture {
    private final Long categoryId;

    private ProductFixture(final Long categoryId) {
        this.categoryId = categoryId;
    }

    public static Builder builder(final CategoryRepository categoryRepository) {
        return new Builder(categoryRepository);
    }

    public Long categoryId() {
        return categoryId;
    }

    public static class Builder {
        private final CategoryRepository categoryRepository;
        private String categoryName = "";

        private Builder(final CategoryRepository categoryRepository) {
            this.categoryRepository = categoryRepository;
        }

        public Builder category(final String name) {
            this.categoryName = name;
            return this;
        }

        public ProductFixture build() {
            Category category = categoryRepository.save(new Category(categoryName));
            return new ProductFixture(category.getId());
        }
    }
}
