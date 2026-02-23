package gift;

import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class CategoryAcceptanceTest extends AcceptanceTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Test
    void 정상_카테고리_등록() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "식품"
                        }
                        """)
                .when()
                .post("/api/categories")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("식품"));

        var categories = categoryRepository.findAll();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("식품");
    }
}
