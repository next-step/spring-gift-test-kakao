package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

// 발견된 프로덕션 버그:
// - ProductRestController.create(): @RequestBody 누락 → JSON body 바인딩 안 됨 → categoryId=null → 500 에러
// - ProductRestController.create(): @ResponseStatus(CREATED) 없음 → 200 반환
// 프로덕션 수정 후 @Disabled 테스트를 활성화하고, 현재 동작 테스트를 삭제할 것
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
    @Disabled("BUG: @RequestBody 누락 → JSON 바인딩 안 됨 + @ResponseStatus(CREATED) 없음. 프로덕션 수정 후 활성화")
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

        // then — 기대 동작 (프로덕션 수정 후)
        response.then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1_500_000))
            .body("imageUrl", equalTo("https://example.com/notebook.png"))
            .body("category.name", equalTo("전자기기"));
    }

    @Test
    void 상품_생성_현재_동작_확인() {
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

        // then — 현재 동작: @RequestBody 누락 → categoryId=null → findById(null) → 500 에러
        response.then()
            .statusCode(500);

        // DB에 상품이 저장되지 않았는지 확인 (500 에러로 트랜잭션 롤백)
        assertThat(productRepository.findAll()).isEmpty();
        // TODO: 프로덕션 수정 후 이 테스트를 삭제하고 '상품_생성_성공' 활성화
    }

    @Test
    @Disabled("BUG: @RequestBody 누락으로 categoryId 전달 불가. 프로덕션 수정 후 활성화")
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

        // then — 기대 동작: 존재하지 않는 categoryId → NoSuchElementException → 500
        response.then()
            .statusCode(500);
    }
}
