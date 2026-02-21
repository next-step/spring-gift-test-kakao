package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryRetrieveAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.clear();
    }

    @Test
    void 데이터가_없을_때_빈_목록_반환() {
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void 등록한_카테고리가_목록에_포함() {
        var category1 = categoryRepository.save(new Category("식품"));
        var category2 = categoryRepository.save(new Category("전자기기"));

        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].id", equalTo(category1.getId().intValue()))
                .body("[0].name", equalTo("식품"))
                .body("[1].id", equalTo(category2.getId().intValue()))
                .body("[1].name", equalTo("전자기기"));
    }
}
