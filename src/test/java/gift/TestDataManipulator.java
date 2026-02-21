package gift;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;

/**
 * 테스트용 데이터를 추가, 삭제할 수 있는 계약
 */
public interface TestDataManipulator {

    /**
     * 새로운 {@link Member} 를 생성
     *
     * @return 영속 context 에서 분리된 {@code Member} entity
     */
    Member addMember(String name, String email);

    /**
     * 새로운 {@link Category} 를 생성
     *
     * @return 영속 context 에서 분리된 {@code Category} entity
     */
    Category addCategory(String name);

    /**
     * 새로운 {@link Product} 를 생성
     *
     * @param categoryId {@code Product} FK 로 설정될 {@link Category} id
     * @return 영속 context 에서 분리된 {@code Product} entity
     * @throws AssertionError {@code categoryId} 에 해당하는 {@code Category} 가 존재하지 않을시 throw
     */
    Product addProduct(String name, int price, String imageUrl, Long categoryId)
            throws AssertionError;

    /**
     * 새로운 {@link Option} 을 생성
     *
     * @param productId {@code Option} FK 로 설정될 {@link Product} id
     * @return 영속 context 에서 분리된 {@code Option} entity
     * @throws AssertionError {@code productId} 에 해당하는 {@code Product} 가 존재하지 않을시 throw
     */
    Option addOption(String name, int quantity, Long productId)
            throws AssertionError;

    /**
     * 모든 데이터를 초기화 (삭제)
     */
    void initAll();

}
