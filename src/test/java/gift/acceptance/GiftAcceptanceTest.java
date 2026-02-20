package gift.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("classpath:sql/test-data.sql")
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 정상적으로_선물을_전달하고_재고가_감소한다() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 1,
                "quantity", 3,
                "receiverId", 2,
                "message", "선물입니다"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(200);

        Integer stock = jdbcTemplate.queryForObject(
            "SELECT quantity FROM option WHERE id = 1", Integer.class);
        assertThat(stock).isEqualTo(7);
    }

    @Test
    void 재고가_부족하면_선물_전달에_실패하고_재고가_변하지_않는다() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 2,
                "quantity", 5,
                "receiverId", 2,
                "message", "선물"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(not(200));

        Integer stock = jdbcTemplate.queryForObject(
            "SELECT quantity FROM option WHERE id = 2", Integer.class);
        assertThat(stock).isEqualTo(1);
    }

    @Test
    void 존재하지_않는_옵션으로_선물_전달에_실패한다() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 999,
                "quantity", 1,
                "receiverId", 2,
                "message", "선물"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(not(200));
    }

    @Test
    void 수량이_0이면_선물_전달에_실패한다() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 1,
                "quantity", 0,
                "receiverId", 2,
                "message", "선물"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(not(200));
    }

    @Test
    void 수량이_음수이면_선물_전달에_실패한다() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 1,
                "quantity", -5,
                "receiverId", 2,
                "message", "선물"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(not(200));

        Integer stock = jdbcTemplate.queryForObject(
            "SELECT quantity FROM option WHERE id = 1", Integer.class);
        assertThat(stock).isEqualTo(10);
    }

    @Test
    void 존재하지_않는_수신자에게_선물_전달에_실패한다() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 1,
                "quantity", 1,
                "receiverId", 999,
                "message", "선물"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(not(200));
    }

    @Test
    void 동시에_선물을_보내도_재고가_음수가_되지_않는다() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    int statusCode = given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", 1)
                        .body(Map.of(
                            "optionId", 1,
                            "quantity", 1,
                            "receiverId", 2,
                            "message", "동시전달"
                        ))
                    .when()
                        .post("/api/gifts")
                    .then()
                        .extract().statusCode();
                    if (statusCode == 200) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        Integer stock = jdbcTemplate.queryForObject(
            "SELECT quantity FROM option WHERE id = 1", Integer.class);
        assertThat(stock).isGreaterThanOrEqualTo(0);
        assertThat(successCount.get() + stock).isEqualTo(10);
    }
}
