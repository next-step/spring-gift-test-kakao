package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.GiftDelivery;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GiftRestControllerTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private GiftDelivery giftDelivery;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Category category;
    private Product product;
    private Option option;
    private Member sender;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        category = categoryRepository.save(new Category("식품"));
        product = productRepository.save(new Product("아메리카노", 4500, "http://image.url", category));
        option = optionRepository.save(new Option("ICE", 10, product));
        sender = memberRepository.save(new Member("홍길동", "hong@test.com"));
    }

    @AfterEach
    void tearDown() {
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("유효한 요청으로 선물을 보내면 200 OK와 재고가 차감된다")
    void give_validRequest_returnsOkAndDecreasesStock() {
        // when
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(Map.of(
                "optionId", option.getId(),
                "quantity", 3,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(200);

        // then
        Option updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(7);
        verify(giftDelivery).deliver(any());
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 선물을 보내면 500 에러가 발생한다")
    void give_nonExistentOption_returns500() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(Map.of(
                "optionId", 9999L,
                "quantity", 3,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 요청하면 500 에러가 발생한다")
    void give_insufficientStock_returns500() {
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(Map.of(
                "optionId", option.getId(),
                "quantity", 15,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);

        // 재고 변화 없음 확인 (트랜잭션 롤백)
        Option unchangedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(unchangedOption.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Member-Id 헤더가 없으면 400 에러가 발생한다")
    void give_missingMemberIdHeader_returns400() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "optionId", option.getId(),
                "quantity", 3,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("재고와 동일한 수량을 요청하면 200 OK와 재고가 0이 된다")
    void give_exactStock_returnsOkAndStockBecomesZero() {
        // when
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(Map.of(
                "optionId", option.getId(),
                "quantity", 10,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(200);

        // then
        Option updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(0);
    }
}
