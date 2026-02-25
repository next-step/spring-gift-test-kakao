package gift;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

@AcceptanceTest
class CategoryRetrieveAcceptanceTest {

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
    @Sql("/sql/카테고리_두개_등록.sql")
    void 등록한_카테고리가_목록에_포함() {
        List<Map<String, Object>> categories = RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .extract()
                .as(new TypeRef<>() {});

        assertThat(categories)
                .extracting(c -> c.get("name"))
                .containsExactlyInAnyOrder("식품", "전자기기");
    }
}
