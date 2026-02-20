package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("카테고리 생성 요청 시 200 응답과 자동 생성된 id가 반환된다")
    @Sql(scripts = "/data/category-acceptance/카테고리_생성_성공.sql", executionPhase = BEFORE_TEST_METHOD)
    void 카테고리_생성_성공() {
        // Given: 카테고리가 없는 빈 상태
        var request = Map.of("name", "전자기기");

        // When: 카테고리 생성 API 호출
        // Then: 200 OK, id가 자동 생성되어 반환된다
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/api/categories")
                .then().log().all()
                    .statusCode(200)
                    .body("id", notNullValue());
    }

    @Test
    @DisplayName("카테고리를 생성하면 목록 조회 시 포함된다")
    @Sql(scripts = "/data/category-acceptance/카테고리_생성_후_목록에서_조회된다.sql", executionPhase = BEFORE_TEST_METHOD)
    void 카테고리_생성_후_목록에서_조회된다() {
        // Given: 카테고리가 없는 빈 상태
        var request = Map.of("name", "전자기기");

        // When: 카테고리 생성
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/api/categories")
                .then().log().all()
                    .statusCode(200);

        // When: 카테고리 목록 조회
        // Then: 생성된 카테고리가 목록에 1건 존재한다 (상태 변화 증명)
        RestAssured
                .given().log().all()
                .when()
                    .get("/api/categories")
                .then().log().all()
                    .statusCode(200)
                    .body("size()", equalTo(1));
    }

    @Test
    @DisplayName("카테고리가 여러 건 있을 때 전체 목록이 조회된다")
    @Sql(scripts = "/data/category-acceptance/카테고리_여러건_조회_성공.sql", executionPhase = BEFORE_TEST_METHOD)
    void 카테고리_여러건_조회_성공() {
        // Given: 3건의 카테고리가 존재하는 상태 (seed.sql 기반)

        // When: 카테고리 목록 조회 API 호출
        // Then: 200 OK, 3건 모두 반환되며 id와 name이 정확하다
        RestAssured
                .given().log().all()
                .when()
                    .get("/api/categories")
                .then().log().all()
                    .statusCode(200)
                    .body("size()", equalTo(3))
                    .body("id", hasItems(1, 2, 3))
                    .body("name", hasItems("전자기기", "의류", "식품"));
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 배열이 반환된다")
    @Sql(scripts = "/data/category-acceptance/카테고리가_없으면_빈_배열이_반환된다.sql", executionPhase = BEFORE_TEST_METHOD)
    void 카테고리가_없으면_빈_배열이_반환된다() {
        // Given: 카테고리가 없는 빈 상태

        // When: 카테고리 목록 조회 API 호출
        // Then: 200 OK, 빈 배열 반환
        RestAssured
                .given().log().all()
                .when()
                    .get("/api/categories")
                .then().log().all()
                    .statusCode(200)
                    .body("size()", equalTo(0));
    }
}
