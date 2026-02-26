package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryApiTest {

    @LocalServerPort
    int port;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 카테고리_생성_성공() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "뷰티"
                }
                """)
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);

        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).anyMatch(c -> c.getName().equals("뷰티"));
    }
}
