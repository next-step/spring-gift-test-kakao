package gift.acceptance;

import gift.application.CreateProductRequest;
import gift.fixture.ProductFixture;
import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 상품을_생성하면_목록_조회_시_조회된다() {
        // given
        ProductFixture fixture = productFixtureBuilder()
                .category("교환권")
                .build();
        CreateProductRequest request = new CreateProductRequest(
                "아이스 아메리카노",
                4500,
                "https://example.com/image.png",
                fixture.categoryId()
        );

        // when — 상품 생성
        ExtractableResponse<Response> createResponse =
                RestAssured.given().log().all()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when()
                        .post("/api/products")
                        .then().log().all()
                        .statusCode(HttpStatus.OK.value())
                        .extract();

        Long createdId = createResponse.jsonPath().getLong("id");

        // then — 목록 조회로 생성 확인
        ExtractableResponse<Response> listResponse = RestAssured.given().log().all()
                .when()
                .get("/api/products")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract();

        List<Long> ids = listResponse.jsonPath().getList("id", Long.class);
        assertThat(ids).containsExactly(createdId);
    }

    @Test
    void 존재하지_않는_카테고리로_상품을_생성하면_실패한다() {
        // given
        ProductFixture fixture = productFixtureBuilder()
                .category("교환권")
                .build();
        Long nonExistentCategoryId = fixture.categoryId() + 1;
        CreateProductRequest request = new CreateProductRequest(
                "아이스 아메리카노",
                4500,
                "https://example.com/image.png",
                nonExistentCategoryId
        );

        // when & then
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/products")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private ProductFixture.Builder productFixtureBuilder() {
        return ProductFixture.builder(categoryRepository);
    }
}
