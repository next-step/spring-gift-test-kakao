package gift.application;

import gift.error.ProductErrorCode;
import gift.error.ProductException;
import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(final ProductRepository productRepository, final CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product create(final CreateProductRequest request) {
        final Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));
        final Product product = new Product(request.getName(), request.getPrice(), request.getImageUrl(), category);
        return productRepository.save(product);
    }

    public List<Product> retrieve() {
        return productRepository.findAll();
    }
}
