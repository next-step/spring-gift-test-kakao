package gift;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import gift.application.request.CreateProductRequest;
import gift.model.Category;
import gift.model.Product;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductAcceptanceTest extends AcceptanceTestSupport {

    Category existingCategory;

    @BeforeEach
    void setUp() {
        super.setupRestAssured();

        // 존재하는 카테고리
        existingCategory = super.addCategory(
                "Already existing category"
        );
    }

    @AfterEach
    void tearDown() {
        super.initAll();
        existingCategory = null;
    }

    @Test
    @DisplayName("사용자가 상품을 추가한다")
    void createProduct() {
        // Given: 카테고리가 존재한다 (@BeforeEach에서 준비)

        // When: 상품 추가 API를 호출한다
        CreateProductRequest request = new CreateProductRequest(
                "new product", 100, "https://sample.com",
                existingCategory.getId()
        );
        Response response = postProduct(request);

        // Then: 상태 코드 200, 응답에 핵심 필드 포함
        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo(request.name()))
                .body("price", equalTo(request.price()))
                .body("imageUrl", equalTo(request.imageUrl()))
                .body("category.id", equalTo(request.categoryId().intValue()));
    }

    private static Response postProduct(CreateProductRequest request) {
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/products");
    }

    @Test
    @DisplayName("사용자가 상품을 조회한다")
    void retrieveProducts() {
        // Given: 상품이 존재한다
        Product product = super.addProduct(
                "Test product", 100, "https://sample.com",
                existingCategory.getId()
        );

        // When: 상품 조회 API를 호출한다
        Response response = getProduct();

        // Then: 상태 코드 200, 목록에 상품이 포함되어 있다
        response.then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("id", hasItem(product.getId().intValue()))
                .body("name", hasItem(product.getName()))
                .body("price", hasItem(product.getPrice()))
                .body("imageUrl", hasItem(product.getImageUrl()))
                .body("category.id", hasItem(existingCategory.getId().intValue()));
    }

    private static Response getProduct() {
        return given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/products");
    }

    @Test
    @DisplayName("사용자가 상품을 추가하고 조회한다")
    void createAndRetrieveProducts() {
        // Given: 카테고리가 존재한다 (@BeforeEach에서 준비)

        // When: 상품을 추가한다
        CreateProductRequest request = new CreateProductRequest(
                "new product 22", 100, "https://sample.com",
                existingCategory.getId()
        );

        //noinspection WrapperTypeMayBePrimitive
        Long addedProductId = postProduct(request).then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("id");

        // When: 상품을 조회한다
        Response response = getProduct();

        // Then: 방금 추가한 상품이 목록에 포함되어 있다
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("id", hasItem(addedProductId.intValue()))
                .body("name", hasItem(request.name()))
                .body("price", hasItem(request.price()))
                .body("imageUrl", hasItem(request.imageUrl()))
                .body("category.id", hasItem(request.categoryId().intValue()));
    }

    @Test
    @DisplayName("상품 추가 시 카테고리 id 가 제공되지 않으면 에러가 발생한다.")
    void createProductWithNullCategoryId() {
        // Given : 없음

        // When : categoryId 가 누락된 체 요청한다.
        CreateProductRequest request = new CreateProductRequest(
                "name", 10, "url", null
        );
        Response response = postProduct(request);

        // Then : 에러 응답 (400)
        response.then()
                .statusCode(400);
    }

    @Test
    @DisplayName("상품 추가 시 존재하지 않는 카테고리 id 가 제공되면 에러가 발생한다.")
    void createProductWithNotExistingCategoryId() {
        // Given : 존재하지 않는 카테고리 ID
        long notExistingCategoryId = Long.MAX_VALUE;

        // When: 존재하지 않는 카테고리 ID 로 상품 추가를 요청한다
        CreateProductRequest request = new CreateProductRequest(
                "name", 10, "url", notExistingCategoryId
        );
        Response response = postProduct(request);

        // Then : 에러 응답 (404)
        response.then()
                .statusCode(404);
    }

    @Test
    @DisplayName("상품 가격이 음수면 에러가 발생한다.")
    void createProductWithNegativePrice() {
        // Given: 카테고리가 존재한다 (@BeforeEach에서 준비)

        // When: 음수 가격으로 상품 추가를 요청한다
        CreateProductRequest request = new CreateProductRequest(
                "name", -10, "url", existingCategory.getId()
        );
        Response response = postProduct(request);

        // Then: 에러 응답 (400)
        response.then()
                .statusCode(400);
    }
}
