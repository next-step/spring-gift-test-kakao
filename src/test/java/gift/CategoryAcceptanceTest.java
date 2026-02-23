package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

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
    void 카테고리_목록_조회_빈_목록() {
        // given — 데이터 없음

        // when & then
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    void 카테고리_목록_조회_N개_존재() {
        // given
        var cat1 = categoryRepository.save(new Category("전자기기"));
        var cat2 = categoryRepository.save(new Category("식품"));

        // when & then
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].id", equalTo(cat1.getId().intValue()))
            .body("[0].name", equalTo("전자기기"))
            .body("[1].id", equalTo(cat2.getId().intValue()))
            .body("[1].name", equalTo("식품"));
    }

    @Test
    void 카테고리_생성_성공() {
        // given
        var request = Map.of("name", "전자기기");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/categories");

        // then
        var id = response.then()
            .statusCode(200)
            .body("name", equalTo("전자기기"))
            .extract().jsonPath().getLong("id");

        var saved = categoryRepository.findById(id).orElseThrow();
        assertThat(saved.getName()).isEqualTo("전자기기");
    }
}
