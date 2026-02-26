package gift;

import gift.model.Option;
import gift.model.OptionRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftApiTest {

    @LocalServerPort
    int port;

    @Autowired
    OptionRepository optionRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql",
        "/sql/gift/success.sql"})
    @Test
    void 선물_보내기_성공() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "optionId": 1,
                    "quantity": 3,
                    "receiverId": 2,
                    "message": "생일 축하해"
                }
                """)
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(200);

        Option updated = optionRepository.findById(1L).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7);
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql",
        "/sql/gift/exact-quantity.sql"})
    @Test
    void 재고와_요청_수량이_같으면_재고가_0이_된다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "optionId": 1,
                    "quantity": 5,
                    "receiverId": 2,
                    "message": "선물"
                }
                """)
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(200);

        Option updated = optionRepository.findById(1L).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(0);
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql",
        "/sql/gift/insufficient-stock.sql"})
    @Test
    void 재고_부족_시_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "optionId": 1,
                    "quantity": 5,
                    "receiverId": 2,
                    "message": "선물"
                }
                """)
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);

        Option unchanged = optionRepository.findById(1L).orElseThrow();
        assertThat(unchanged.getQuantity()).isEqualTo(2);
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 존재하지_않는_옵션으로_선물_보내기_시_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "optionId": 999,
                    "quantity": 1,
                    "receiverId": 2,
                    "message": "선물"
                }
                """)
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 옵션_ID에_잘못된_타입을_보내면_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "optionId": "잘못된값",
                    "quantity": 1,
                    "receiverId": 2,
                    "message": "선물"
                }
                """)
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(400);
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql",
        "/sql/gift/zero-stock.sql"})
    @Test
    void 재고가_0일_때_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "optionId": 1,
                    "quantity": 1,
                    "receiverId": 2,
                    "message": "선물"
                }
                """)
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);

        Option unchanged = optionRepository.findById(1L).orElseThrow();
        assertThat(unchanged.getQuantity()).isEqualTo(0);
    }
}
