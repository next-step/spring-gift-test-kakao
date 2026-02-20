package gift;

import gift.model.OptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftBehaviorTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OptionRepository optionRepository;

    /**
     * Behavior 1: 선물하기 성공 시 옵션 재고가 감소한다
     *
     * Given: 카테고리, 상품, 옵션(수량=10), 보내는 회원, 받는 회원이 존재
     * When:  POST /api/gifts + Header Member-Id + Body { optionId, quantity: 3, receiverId, message }
     * Then:  HTTP 200 OK / 옵션 수량이 10→7로 감소
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/gift-setup.sql"})
    void should_decrease_option_quantity_when_gift_is_sent_successfully() {
        // When
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Member-Id", "1");

        Map<String, Object> body = Map.of(
            "optionId", 1,
            "quantity", 3,
            "receiverId", 2,
            "message", "선물입니다"
        );

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/gifts",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Void.class
        );

        // Then — HTTP 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Then — DB 상태 변화 검증 (조회 API 미노출이므로 Repository 직접 조회)
        var updatedOption = optionRepository.findById(1L).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(7);
    }
}
