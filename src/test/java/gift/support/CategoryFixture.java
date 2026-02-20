package gift.support;

import gift.model.Category;
import gift.model.CategoryRepository;
import org.springframework.stereotype.Component;

@Component
public class CategoryFixture {

    private final CategoryRepository categoryRepository;

    public CategoryFixture(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private String name = "테스트카테고리";

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Category build() {
            return categoryRepository.save(new Category(name));
        }
    }
}