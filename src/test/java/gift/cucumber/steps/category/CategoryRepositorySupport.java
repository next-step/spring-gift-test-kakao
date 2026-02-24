package gift.cucumber.steps.category;

import gift.model.Category;
import gift.model.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class CategoryRepositorySupport {
    private final CategoryRepository categoryRepository;

    public CategoryRepositorySupport(final CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category seedCategory(final String name) {
        return categoryRepository.save(new Category(name));
    }

    public void seedCategories(final List<String> names) {
        names.forEach(name -> categoryRepository.save(new Category(name)));
    }

    public void assertEmpty() {
        assertThat(categoryRepository.count()).isZero();
    }

    public void assertSaved(final String name) {
        assertThat(categoryRepository.findAll())
            .extracting(Category::getName)
            .contains(name);
    }
}
