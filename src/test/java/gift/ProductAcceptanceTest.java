package gift;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    Category category;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        category = categoryRepository.save(new Category("테스트 카테고리"));
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자가 상품을 추가한다")
    void createProduct() {
        // Given: 카테고리가 존재한다 (@BeforeEach에서 준비)

        // When: 상품 추가 API를 호출한다
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "https://example.com/image.jpg",
                        "categoryId", category.getId()
                ))
                .when()
                .post("/api/products");

        // Then: 상태 코드 200, 응답에 핵심 필드 포함
        response.then()
                .statusCode(200)
                .body("name", equalTo("아메리카노"));
    }

    @Test
    @DisplayName("사용자가 상품을 조회한다")
    void retrieveProducts() {
        // Given: 상품이 존재한다
        productRepository.save(new Product("테스트 상품", 1000, "https://test.com/image.jpg", category));

        // When: 상품 조회 API를 호출한다
        var response = RestAssured.given()
                .when()
                .get("/api/products");

        // Then: 상태 코드 200, 목록에 상품이 포함되어 있다
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("사용자가 상품을 추가하고 조회한다")
    void createAndRetrieveProduct() {
        // Given: 카테고리가 존재한다 (@BeforeEach에서 준비)

        // When: 상품을 추가한다
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "카페라떼",
                        "price", 5000,
                        "imageUrl", "https://example.com/latte.jpg",
                        "categoryId", category.getId()
                ))
                .when()
                .post("/api/products")
                .then()
                .statusCode(200);

        // When: 상품을 조회한다
        var response = RestAssured.given()
                .when()
                .get("/api/products");

        // Then: 방금 추가한 상품이 목록에 포함되어 있다
        response.then()
                .statusCode(200)
                .body("name", hasItem("카페라떼"));
    }

    @Test
    @DisplayName("상품 추가 시 카테고리가 유효하지 않으면 에러가 발생한다")
    void createProductWithInvalidCategoryIdFails() {
        // Given: 존재하지 않는 카테고리 ID
        // categoryId null → IllegalArgumentException (findById(null))
        // categoryId 미존재 → NoSuchElementException (orElseThrow)
        // 글로벌 예외 핸들러 부재로 두 경우 모두 500 응답

        // When: 존재하지 않는 카테고리 ID로 상품 추가를 요청한다
        var response1 = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "에러 상품",
                        "price", 1000,
                        "imageUrl", "https://example.com/error.jpg",
                        "categoryId", 999999L
                ))
                .when()
                .post("/api/products");

        // Then: 에러 응답
        response1.then()
                .statusCode(500);

        // When: 카테고리 ID가 포함되지 않은 상태로 상품 추가를 요청한다.
        var response2 = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "에러 상품",
                        "price", 1000,
                        "imageUrl", "https://example.com/error.jpg"
                ))
                .when()
                .post("/api/products");

        // Then: 에러 응답
        response2.then()
                .statusCode(500);
    }

    @Test
    @DisplayName("상품 가격이 음수면 에러가 발생한다.")
    void createProductWithNegativePriceCurrentlySucceeds() {
        // Given: 카테고리가 존재한다 (@BeforeEach에서 준비)

        // When: 음수 가격으로 상품 추가를 요청한다
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "음수 가격 상품",
                        "price", -1000,
                        "imageUrl", "https://example.com/negative.jpg",
                        "categoryId", category.getId()
                ))
                .when()
                .post("/api/products");

        // Then: 에러 응답
        // TODO: 현재 코드에는 price 유효성 검증이 없어 200 이 반환된다.
        //      리팩토링 시 유효성 검증을 추가하고, 이 테스트를 4xx 에러 검증으로 변경해야 한다.
        response.then()
                .statusCode(500);
    }
}