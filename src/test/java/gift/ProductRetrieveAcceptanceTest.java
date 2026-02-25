package gift;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.hasSize;

@AcceptanceTest
class ProductRetrieveAcceptanceTest {

    @Test
    void 데이터가_없을_때_빈_목록_반환() {
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Sql("/sql/상품_두개_등록.sql")
    void 등록한_상품이_목록에_포함_카테고리_중첩_응답() {
        List<Map<String, Object>> products = RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .extract()
                .as(new TypeRef<>() {});

        assertThat(products)
                .extracting(
                        p -> p.get("name"),
                        p -> p.get("price"),
                        p -> p.get("imageUrl"),
                        p -> ((Map<?, ?>) p.get("category")).get("name")
                )
                .containsExactlyInAnyOrder(
                        tuple("케이크", 30000, "https://example.com/cake.jpg", "식품"),
                        tuple("초콜릿", 15000, "https://example.com/choco.jpg", "식품")
                );
    }
}
