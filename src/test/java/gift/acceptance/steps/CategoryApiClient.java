package gift.acceptance.steps;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class CategoryApiClient {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Response 카테고리를_등록한다(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories");
    }

    public Response 카테고리를_이름없이_등록한다() {
        return given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/categories");
    }

    public Response 카테고리_목록을_조회한다() {
        return given()
        .when()
            .get("/api/categories");
    }

    public Long 카테고리를_DB에_등록한다(String name) {
        jdbcTemplate.update("INSERT INTO category (name) VALUES (?)", name);
        return jdbcTemplate.queryForObject(
            "SELECT id FROM category WHERE name = ?", Long.class, name);
    }
}
