package gift;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestSupport implements TestDataManipulator {

    @LocalServerPort
    private int localServerPort;

    @Autowired
    private AcceptanceTestDataManipulator dataManipulator;

    @Override
    public final Member addMember(String name, String email) {
        return dataManipulator.addMember(name, email);
    }

    @Override
    public final Category addCategory(String name) {
        return dataManipulator.addCategory(name);
    }

    @Override
    public final Product addProduct(String name, int price, String imageUrl, Long categoryId) {
        return dataManipulator.addProduct(name, price, imageUrl, categoryId);
    }

    @Override
    public final Option addOption(String name, int quantity, Long productId) {
        return dataManipulator.addOption(name, quantity, productId);
    }

    @Override
    public final void initAll() {
        dataManipulator.initAll();
    }

    /**
     * {@code RestAssured} 의 요청 port 를 Spring server port 로 설정
     * <p>
     * {@code RestAssured} 의 모든 요청 & 응답을 {@code System.out} 에 출력되도록 설정
     */
    protected final void setupRestAssured() {
        RestAssured.port = this.localServerPort;

        // 요청 날렸을 때 url, query param, request body 확인하기 위한 구성품들
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

}
