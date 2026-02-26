package gift.step;

import static io.restassured.RestAssured.given;

import gift.TestDataManipulator;
import gift.application.request.CreateProductRequest;
import gift.model.Product;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

/**
 * 상품 관련 Step Definition.
 * <p>
 * 상품 생성 Given step은 gift.feature의 Background에서도 재사용된다. 카테고리 ID는
 * {@link AcceptanceContext#getLastCategoryId()}를 통해 암묵적으로 참조한다.
 */
public class ProductStepDefinitions {

    private final AcceptanceContext context;
    private final TestDataManipulator dataManipulator;

    public ProductStepDefinitions(AcceptanceContext context, TestDataManipulator dataManipulator) {
        this.context = context;
        this.dataManipulator = dataManipulator;
    }

    // --- Given ---

    @Given("상품 {string}가 등록되어 있다.")
    public void productExists(String name) {
        productExistsWithPrice(name, 1000);
    }

    @Given("상품 {string}가 가격 {int}으로 등록되어 있다.")
    public void productExistsWithPrice(String name, int price) {
        productExistsWithPriceAndImage(name, price, "https://default-image.com");
    }

    @Given("상품 {string}가 가격 {int}, 이미지 {string}으로 등록되어 있다.")
    public void productExistsWithPriceAndImage(String name, int price, String imageUrl) {
        Long categoryId = context.getLastCategoryId();
        Product product = dataManipulator.addProduct(name, price, imageUrl, categoryId);

        context.putProductId(name, product.getId());
    }

    // --- When ---

    @When("상품명 {string}, 가격 {int}, 이미지 {string}으로 상품을 추가한다")
    public void addProduct(String name, int price, String imageUrl) {
        Long categoryId = context.getLastCategoryId();

        CreateProductRequest request = new CreateProductRequest(
                name, price, imageUrl, categoryId
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when()
                        .post("/api/products")
        );
    }

    @When("상품 목록을 조회한다")
    public void listProducts() {
        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .when()
                        .get("/api/products")
        );
    }

    @When("카테고리 id 없이 상품 추가를 요청한다")
    public void addProductWithoutCategoryId() {
        addProductWithoutCategoryIdDetailed("테스트 상품", 100, "https://sample.com");
    }

    @When("상품명 {string}, 가격 {int}, 이미지 {string}으로 카테고리 id 없이 상품 추가를 요청한다")
    public void addProductWithoutCategoryIdDetailed(String name, int price, String imageUrl) {
        CreateProductRequest request = new CreateProductRequest(
                name, price, imageUrl, null
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when()
                        .post("/api/products")
        );
    }

    @When("존재하지 않는 카테고리 id로 상품 추가를 요청한다")
    public void addProductWithNonExistentCategoryId() {
        addProductWithNonExistentCategoryIdDetailed("테스트 상품", 100, "https://sample.com");
    }

    @When("상품명 {string}, 가격 {int}, 이미지 {string}으로 존재하지 않는 카테고리 id로 상품 추가를 요청한다")
    public void addProductWithNonExistentCategoryIdDetailed(
            String name, int price, String imageUrl
    ) {
        Long notExistingId = context.getNotExistingId();

        CreateProductRequest request = new CreateProductRequest(
                name, price, imageUrl, notExistingId
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when()
                        .post("/api/products")
        );
    }

    @When("가격 {int}으로 상품 추가를 요청한다")
    public void addProductWithPrice(int price) {
        addProduct("테스트 상품", price, "https://sample.com");
    }
}
