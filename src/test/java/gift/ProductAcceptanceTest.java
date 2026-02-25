package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@AcceptanceTest
class ProductAcceptanceTest {

    @Test
    @Sql("/sql/상품_등록_카테고리_준비.sql")
    void 정상_상품_등록() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "아이폰 16",
                            "price": 1500000,
                            "imageUrl": "https://example.com/iphone.jpg",
                            "categoryId": 1
                        }
                        """)
                .when()
                .post("/api/products")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("아이폰 16"))
                .body("price", equalTo(1500000))
                .body("imageUrl", equalTo("https://example.com/iphone.jpg"))
                .body("category.id", equalTo(1))
                .body("category.name", equalTo("식품"));

        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("아이폰 16"))
                .body("[0].price", equalTo(1500000));
    }

    @Test
    void 존재하지_않는_카테고리로_등록_시도() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "아이폰 16",
                            "price": 1500000,
                            "imageUrl": "https://example.com/iphone.jpg",
                            "categoryId": 999999
                        }
                        """)
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }
}
