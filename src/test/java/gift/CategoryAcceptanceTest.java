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
        categoryRepository.save(new Category("전자기기"));
        categoryRepository.save(new Category("식품"));

        // when & then
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].id", notNullValue())
            .body("[0].name", notNullValue())
            .body("[1].id", notNullValue())
            .body("[1].name", notNullValue());
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
        response.then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("전자기기"));
    }

    @Test
    void 카테고리_생성_현재_동작_확인() {
        // given
        var request = Map.of("name", "전자기기");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/categories");

        // then — 현재 동작 확인
        response.then()
            .statusCode(200)  // @ResponseStatus(CREATED) 없어서 기본값 200
            .body("id", notNullValue())
            .body("name", equalTo("전자기기"));
    }

    @Test
    void 카테고리_생성_후_DB_저장_확인() {
        // given
        var request = Map.of("name", "전자기기");

        // when
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/categories");

        // then — DB에 실제로 저장되었는지 확인
        var categories = categoryRepository.findAll();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getId()).isNotNull();
        assertThat(categories.get(0).getName()).isEqualTo("전자기기");
    }
}
