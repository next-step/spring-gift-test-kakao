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
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 역순 삭제
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 선물_전송_성공() {
        // given
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        var category = categoryRepository.save(new Category("전자기기"));
        var product = productRepository.save(new Product("노트북", 1_500_000, "https://example.com/notebook.png", category));
        var option = optionRepository.save(new Option("기본 옵션", 10, product));

        var request = Map.of(
            "optionId", option.getId(),
            "quantity", 3,
            "receiverId", receiver.getId(),
            "message", "생일 축하해!"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(request)
        .when()
            .post("/api/gifts");

        // then — Layer 1: 상태 코드
        response.then()
            .statusCode(200);

        // then — Layer 3: 상태 변화 (DB) — 재고 감소 확인
        var updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(7);  // 10 - 3 = 7
    }

    @Test
    void 선물_전송_실패_헤더_누락() {
        // given
        var request = Map.of(
            "optionId", 1L,
            "quantity", 1,
            "receiverId", 1L,
            "message", "선물"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/gifts");

        // then — Member-Id 헤더 누락 → MissingRequestHeaderException → 400
        response.then()
            .statusCode(400);
    }

    @Test
    void 선물_전송_실패_존재하지_않는_옵션() {
        // given
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var request = Map.of(
            "optionId", 9999L,
            "quantity", 1,
            "receiverId", 1L,
            "message", "선물"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(request)
        .when()
            .post("/api/gifts");

        // then — 존재하지 않는 optionId → NoSuchElementException → 500
        response.then()
            .statusCode(500);
    }

    @Test
    void 선물_전송_실패_재고_부족() {
        // given
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        var category = categoryRepository.save(new Category("전자기기"));
        var product = productRepository.save(new Product("노트북", 1_500_000, "https://example.com/notebook.png", category));
        var option = optionRepository.save(new Option("기본 옵션", 2, product));

        var request = Map.of(
            "optionId", option.getId(),
            "quantity", 5,
            "receiverId", receiver.getId(),
            "message", "선물"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(request)
        .when()
            .post("/api/gifts");

        // then — 재고 2개인데 5개 요청 → IllegalStateException → 500
        response.then()
            .statusCode(500);

        // 재고가 변경되지 않았는지 확인 (트랜잭션 롤백)
        var unchangedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(unchangedOption.getQuantity()).isEqualTo(2);
    }

    @Test
    void 선물_전송_실패_발신자_미존재() {
        // given
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        var category = categoryRepository.save(new Category("전자기기"));
        var product = productRepository.save(new Product("노트북", 1_500_000, "https://example.com/notebook.png", category));
        var option = optionRepository.save(new Option("기본 옵션", 10, product));

        var request = Map.of(
            "optionId", option.getId(),
            "quantity", 1,
            "receiverId", receiver.getId(),
            "message", "선물"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 9999L)  // DB에 없는 memberId
            .body(request)
        .when()
            .post("/api/gifts");

        // then — FakeGiftDelivery에서 발신자 조회 실패 → NoSuchElementException → 500
        response.then()
            .statusCode(500);

        // 재고가 변경되지 않았는지 확인 (트랜잭션 롤백)
        var unchangedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(unchangedOption.getQuantity()).isEqualTo(10);
    }
}
