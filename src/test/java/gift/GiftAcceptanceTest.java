package gift;

import gift.application.GiveGiftRequest;
import gift.fixture.GiftFixture;
import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 선물하기가_정상적으로_처리되면_옵션_재고가_차감된다() {
        // given
        int initialQuantity = 10;
        int giftQuantity = 3;
        GiftFixture fixture = giftFixtureBuilder()
                .option("", initialQuantity)
                .build();
        GiveGiftRequest request = new GiveGiftRequest(fixture.optionId(), giftQuantity, fixture.receiverId(), "생일 축하해!");

        // when
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", fixture.senderId())
                .body(request)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .statusCode(HttpStatus.OK.value());

        // then — 조회 API 미제공으로 DB에서 재고 차감 직접 확인
        Integer remainingQuantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM option WHERE id = ?", Integer.class, fixture.optionId());
        assertThat(remainingQuantity).isEqualTo(initialQuantity - giftQuantity);
    }

    @Test
    void 재고보다_많은_수량을_선물하면_실패하고_재고는_변경되지_않는다() {
        // given
        int initialQuantity = 10;
        int giftQuantity = 11;
        GiftFixture fixture = giftFixtureBuilder()
            .option("", initialQuantity)
            .build();
        GiveGiftRequest request = new GiveGiftRequest(fixture.optionId(), giftQuantity, fixture.receiverId(), "선물");

        // when
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", fixture.senderId())
                .body(request)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // then — 트랜잭션 롤백으로 재고가 변경되지 않았는지 확인
        Integer remainingQuantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM option WHERE id = ?", Integer.class, fixture.optionId());
        assertThat(remainingQuantity).isEqualTo(fixture.initialQuantity());
    }

    @Test
    void 존재하지_않는_옵션으로_선물하면_실패한다() {
        // given
        GiftFixture fixture = giftFixtureBuilder().build();
        Long nonExistentOptionId = fixture.optionId() + 1;
        GiveGiftRequest request = new GiveGiftRequest(nonExistentOptionId, 1, fixture.receiverId(), "선물");

        // when & then
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", fixture.senderId())
                .body(request)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void 존재하지_않는_발신자로_선물하면_실패한다() {
        // given
        GiftFixture fixture = giftFixtureBuilder().build();
        Long nonExistentMemberId = Math.max(fixture.senderId(), fixture.receiverId()) + 1;
        GiveGiftRequest request = new GiveGiftRequest(fixture.optionId(), 1, fixture.receiverId(), "선물");

        // when & then
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", nonExistentMemberId)
                .body(request)
                .when()
                .post("/api/gifts")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private GiftFixture.Builder giftFixtureBuilder() {
        return GiftFixture.builder(memberRepository, categoryRepository, productRepository, optionRepository);
    }
}
