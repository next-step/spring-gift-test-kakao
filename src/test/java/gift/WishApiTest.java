package gift;

import gift.model.Wish;
import gift.model.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WishApiTest {

    @LocalServerPort
    int port;

    @Autowired
    WishRepository wishRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql",
        "/sql/wish/success.sql"})
    @Test
    void 위시리스트_추가_성공() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "productId": 1
                }
                """)
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(200);

        List<Wish> wishes = wishRepository.findAll();
        assertThat(wishes).hasSize(1);
        assertThat(wishes.get(0).getMember().getId()).isEqualTo(1L);
        assertThat(wishes.get(0).getProduct().getId()).isEqualTo(1L);
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 존재하지_않는_상품에_위시리스트_추가_시_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "productId": 999
                }
                """)
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(500);

        List<Wish> wishes = wishRepository.findAll();
        assertThat(wishes).isEmpty();
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 상품_ID에_잘못된_타입을_보내면_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1L)
            .body("""
                {
                    "productId": "잘못된값"
                }
                """)
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(400);

        List<Wish> wishes = wishRepository.findAll();
        assertThat(wishes).isEmpty();
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql",
        "/sql/wish/invalid-member.sql"})
    @Test
    void 존재하지_않는_회원이_위시리스트_추가_시_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 999L)
            .body("""
                {
                    "productId": 1
                }
                """)
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(500);

        List<Wish> wishes = wishRepository.findAll();
        assertThat(wishes).isEmpty();
    }
}
