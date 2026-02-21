package gift.application;

import gift.application.request.CreateCategoryRequest;
import gift.model.Category;
import gift.model.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(final CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category create(final CreateCategoryRequest request) {
        return categoryRepository.save(new Category(request.name()));
    }

    public List<Category> retrieve() {
        return categoryRepository.findAll();
    }
}
