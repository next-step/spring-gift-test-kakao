package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    Member sender;
    Member receiver;
    Category category;
    Product product;
    Option option;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        category = categoryRepository.save(new Category("테스트 카테고리"));
        product = productRepository.save(
                new Product("테스트 상품", 1000, "https://test.com/image.jpg", category));
        option = optionRepository.save(new Option("기본 옵션", 10, product));
    }

    @AfterEach
    void tearDown() {
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자가 선물을 보낸다")
    void giveGift() {
        // Given: 보내는 사람, 받는 사람, 상품, 옵션이 존재한다 (@BeforeEach에서 준비)

        // When: 선물 보내기 API를 호출한다
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "receiverId", receiver.getId(),
                        "message", "생일 축하합니다!"
                ))
                .when()
                .post("/api/gifts");

        // Then: 상태 코드 200
        response.then()
                .statusCode(200);
    }

    @Test
    @DisplayName("존재하지 않는 optionId로 선물을 보내면 에러가 발생한다")
    void giveGiftWithInvalidOptionIdFails() {
        // TODO: 현재 코드에는 optionId 유효성 검증이 없어 200 이 반환된다.
        //      리팩토링 시 유효성 검증을 추가하고, 이 테스트를 4xx 에러 검증으로 변경해야 한다.
        // Given: 존재하지 않는 optionId

        // When: 존재하지 않는 optionId로 선물 보내기를 요청한다
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", 999999L,
                        "quantity", 1,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");

        // Then: NoSuchElementException → 글로벌 예외 핸들러 부재로 500
        response.then()
                .statusCode(500);

        // When: optionId가 포함되지 않은 상태로 선물 보내기를 요청한다.
        var response2 = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "quantity", 1,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");

        // Then: 에러 응답
        response2.then()
                .statusCode(500);
    }

    @Test
    @DisplayName("존재하지 않는 receiverId로 선물을 보내면 에러가 발생한다")
    void giveGiftWithInvalidReceiverIdFails() {
        // TODO: 현재 코드에는 receiverId 유효성 검증이 없어 200 이 반환된다.
        //      리팩토링 시 유효성 검증을 추가하고, 이 테스트를 4xx 에러 검증으로 변경해야 한다.
        // Given: 존재하지 않는 receiverId

        // When: 존재하지 않는 receiverId로 선물 보내기를 요청한다
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "receiverId", 999999L,
                        "message", "생일 축하합니다!"
                ))
                .when()
                .post("/api/gifts");

        // Then: 에러 응답
        response.then()
                .statusCode(500);

        // When: receiverId가 포함되지 않은 상태로 선물 보내기를 요청한다.
        var response2 = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "message", "생일 축하합니다!"
                ))
                .when()
                .post("/api/gifts");

        // Then: 에러 응답
        response2.then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Member-Id 헤더가 없으면 에러가 발생한다")
    void giveGiftWithoutMemberIdHeaderFails() {
        // Given: Member-Id 헤더 없이 요청
        // 참고: @RequestHeader("Member-Id")는 required=true(기본값)이므로
        //       헤더 누락 시 Spring MVC가 MissingRequestHeaderException을 던져 400 응답을 반환한다.
        //       존재하지 않는 Member-Id의 경우, FakeGiftDelivery에서 조회 실패로 500 응답이 발생한다.

        // When: Member-Id 헤더 없이 선물 보내기를 요청한다
        var response1 = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");

        // Then: 헤더 누락으로 400
        response1.then()
                .statusCode(400);

        // When: 존재하지 않는 Member-Id 헤더로 선물 보내기를 요청한다
        var response2 = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 999999L)
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");

        // Then: 에러 응답
        response2.then()
                .statusCode(500);
    }

    @Test
    @DisplayName("재고가 부족한 선물을 보내면 에러가 발생한다")
    void giveGiftWithInsufficientStockFails() {
        // Given: 옵션의 재고는 10개 (@BeforeEach에서 준비)

        // When: 재고보다 많은 수량(100개)으로 선물 보내기를 요청한다
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 100,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");

        // Then: Option.decrease()에서 IllegalStateException → 글로벌 예외 핸들러 부재로 500
        response.then()
                .statusCode(500);
    }
}