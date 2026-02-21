package gift;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인수 테스트용 테스트 데이터 조작기
 */
@Component
@Transactional
public class AcceptanceTestDataManipulator implements TestDataManipulator {

    @Autowired
    private TestMemberRepository memberRepo;

    @Autowired
    private TestCategoryRepository categoryRepo;

    @Autowired
    private TestProductRepository productRepo;

    @Autowired
    private TestOptionRepository optionRepo;

    @Override
    public Member addMember(String name, String email) {
        return memberRepo.save(new Member(name, email));
    }

    @Override
    public Category addCategory(String name) {
        return categoryRepo.save(new Category(name));
    }

    @Override
    public Product addProduct(String name, int price, String imageUrl, Long categoryId) {

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(AssertionError::new);

        return productRepo.save(new Product(
                name, price, imageUrl, category
        ));
    }

    @Override
    public Option addOption(String name, int quantity, Long productId) {

        Product product = productRepo.findById(productId)
                .orElseThrow(AssertionError::new);

        return optionRepo.save(new Option(
                name, quantity, product
        ));
    }

    @Override
    public void initAll() {
        memberRepo.deleteAllInBatch();
        optionRepo.deleteAllInBatch();
        productRepo.deleteAllInBatch();
        categoryRepo.deleteAllInBatch();
    }

    @PostConstruct
    private void checkBeansNonNull() {
        requireNonNulls(
                categoryRepo, productRepo,
                memberRepo, optionRepo
        );
    }

    private static void requireNonNulls(Object... objs) {
        for (Object obj : objs) {
            Objects.requireNonNull(obj);
        }
    }
}
