package gift.cucumber.steps.product;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class ProductRepositorySupport {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductRepositorySupport(final ProductRepository productRepository, final CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public void seedProduct(final String name, final int price, final String imageUrl, final Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        productRepository.save(new Product(name, price, imageUrl, category));
    }

    public void assertEmpty() {
        assertThat(productRepository.count()).isZero();
    }

    public void assertSavedByName(final String name) {
        assertThat(productRepository.findAll())
            .extracting(Product::getName)
            .contains(name);
    }
}
