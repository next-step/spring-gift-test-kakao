package gift.restassured;

import gift.model.Category;
import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

class CategoryRetrieveAcceptanceTest extends AcceptanceTest {

    @Autowired
    CategoryRepository categoryRepository;

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
        categoryRepository.save(new Category("식품"));
        categoryRepository.save(new Category("전자기기"));

        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("식품", "전자기기"));
    }
}
