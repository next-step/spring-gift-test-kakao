package gift.application;

import gift.application.request.CreateOptionRequest;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public Option create(final CreateOptionRequest request) {
        Long productId = request.productId();

        final Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return optionRepository.save(new Option(request.name(), request.quantity(), product));
    }

    public List<Option> retrieve() {
        return optionRepository.findAll();
    }
}
