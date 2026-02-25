package gift;

import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class CategoryStepDefinitions {

    @먼저("{string} 카테고리가 존재한다")
    public void 카테고리가_존재한다(String name) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                SharedContext.DB_URL, SharedContext.DB_USER, SharedContext.DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO category (name) VALUES (?) RETURNING id")) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            SharedContext.putCategoryId(name, rs.getLong("id"));
        }
    }

    @만일("{string} 카테고리를 생성한다")
    public void 카테고리를_생성한다(String name) {
        var response = RestAssured.given().log().all()
                .baseUri(SharedContext.BASE_URL)
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/categories")
                .then().log().all()
                .extract();
        SharedContext.setResponse(response);
    }

    @만일("카테고리를 전체 조회한다")
    public void 카테고리를_전체_조회한다() {
        var response = RestAssured.given().log().all()
                .baseUri(SharedContext.BASE_URL)
                .when()
                .get("/api/categories")
                .then().log().all()
                .extract();
        SharedContext.setResponse(response);
    }
}
