package gift;

import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.config = RestAssured.config()
                .encoderConfig(EncoderConfig.encoderConfig()
                        .defaultCharsetForContentType("UTF-8", ContentType.URLENC));
        databaseCleaner.clear();
    }

    // TODO: CreateCategoryRequest의 setter가 없어서 name 없이 등록 시도하는 테스트가 실패한다. setter 추가 필요
    @Test
    void 정상_카테고리_등록() {
        RestAssured.given()
                .contentType(ContentType.URLENC)
                .formParam("name", "식품")
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
