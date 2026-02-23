package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 역순 삭제 — 다른 테스트 클래스가 생성한 데이터도 정리
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void 상품_목록_조회_빈_목록() {
        // given — 데이터 없음

        // when & then
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    void 상품_목록_조회_N개_존재() {
        // given
        var category = categoryRepository.save(new Category("전자기기"));
        productRepository.save(new Product("노트북", 1_500_000, "https://example.com/notebook.png", category));
        productRepository.save(new Product("키보드", 120_000, "https://example.com/keyboard.png", category));

        // when & then
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].name", equalTo("노트북"))
            .body("[0].price", equalTo(1_500_000))
            .body("[0].imageUrl", equalTo("https://example.com/notebook.png"))
            .body("[0].category.name", equalTo("전자기기"))
            .body("[1].name", equalTo("키보드"))
            .body("[1].price", equalTo(120_000))
            .body("[1].imageUrl", equalTo("https://example.com/keyboard.png"))
            .body("[1].category.name", equalTo("전자기기"));
    }

    @Test
    void 상품_생성_성공() {
        // given
        var category = categoryRepository.save(new Category("전자기기"));
        var request = Map.of(
            "name", "노트북",
            "price", 1_500_000,
            "imageUrl", "https://example.com/notebook.png",
            "categoryId", category.getId()
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        var id = response.then()
            .statusCode(200)
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1_500_000))
            .body("imageUrl", equalTo("https://example.com/notebook.png"))
            .body("category.name", equalTo("전자기기"))
            .extract().jsonPath().getLong("id");

        assertThat(productRepository.findById(id)).isPresent();
    }

    @Test
    void 상품_생성_실패_존재하지_않는_카테고리() {
        // given
        var request = Map.of(
            "name", "노트북",
            "price", 1_500_000,
            "imageUrl", "https://example.com/notebook.png",
            "categoryId", 9999L
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(500);
    }
}
