package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@AcceptanceTest
class CategoryAcceptanceTest {

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
                .body("id", notNullValue());

        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("식품"));
    }
}
