//package gift.acceptance;
//
//import io.restassured.RestAssured;
//import io.restassured.http.ContentType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.test.context.jdbc.Sql;
//
//import java.util.Map;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.Matchers.hasItem;
//import static org.hamcrest.Matchers.not;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Sql("classpath:sql/test-data.sql")
//class ProductAcceptanceTest {
//
//    @LocalServerPort
//    int port;
//
//    @BeforeEach
//    void setUp() {
//        RestAssured.port = port;
//    }
//
//    @Test
//    void 상품을_정상적으로_생성한다() {
//        String name = "새상품";
//
//        given()
//            .contentType(ContentType.JSON)
//            .body(Map.of(
//                "name", name,
//                "price", 5000,
//                "imageUrl", "http://test.com/new.jpg",
//                "categoryId", 100
//            ))
//        .when()
//            .post("/api/products")
//        .then()
//            .statusCode(200);
//
//        given()
//        .when()
//            .get("/api/products")
//        .then()
//            .statusCode(200)
//            .body("name", hasItem(name));
//    }
//
//    @Test
//    void 상품_목록을_조회한다() {
//        given()
//        .when()
//            .get("/api/products")
//        .then()
//            .statusCode(200)
//            .body("name", hasItem("테스트상품"));
//    }
//
//    @Test
//    @Sql("classpath:sql/truncate.sql")
//    void 존재하지_않는_카테고리로_상품_생성에_실패한다() {
//        given()
//            .contentType(ContentType.JSON)
//            .body(Map.of(
//                "name", "상품",
//                "price", 1000,
//                "imageUrl", "http://test.com/img.jpg",
//                "categoryId", 999
//            ))
//        .when()
//            .post("/api/products")
//        .then()
//            .statusCode(not(200));
//    }
//
//    @Test
//    void 가격이_음수이면_상품_생성에_실패한다() {
//        given()
//            .contentType(ContentType.JSON)
//            .body(Map.of(
//                "name", "상품",
//                "price", -1000,
//                "imageUrl", "http://test.com/img.jpg",
//                "categoryId", 100
//            ))
//        .when()
//            .post("/api/products")
//        .then()
//            .statusCode(not(200));
//    }
//
//    @Test
//    void 이름이_null이면_상품_생성에_실패한다() {
//        given()
//            .contentType(ContentType.JSON)
//            .body(Map.of(
//                "price", 1000,
//                "imageUrl", "http://test.com/img.jpg",
//                "categoryId", 100
//            ))
//        .when()
//            .post("/api/products")
//        .then()
//            .statusCode(not(200));
//    }
//
//    @Test
//    void 이름이_빈_문자열이면_상품_생성에_실패한다() {
//        given()
//            .contentType(ContentType.JSON)
//            .body(Map.of(
//                "name", "",
//                "price", 1000,
//                "imageUrl", "http://test.com/img.jpg",
//                "categoryId", 100
//            ))
//        .when()
//            .post("/api/products")
//        .then()
//            .statusCode(not(200));
//    }
//}
