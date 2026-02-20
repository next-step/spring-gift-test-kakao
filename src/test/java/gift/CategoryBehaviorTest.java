package gift;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryBehaviorTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Behavior 6: 카테고리를 생성하면 조회 시 반환된다
     *
     * [현재 행동 — DTO 바인딩 버그]
     * CreateCategoryRequest에 setter가 없어 form/query 파라미터 바인딩이 실패한다.
     * name이 null로 남아 Category(null)이 저장된다.
     * 생성은 성공(HTTP 200)하지만, name이 null인 카테고리가 만들어진다.
     *
     * Given: 없음 (사전 조건 없음)
     * When:  POST /api/categories (query 파라미터: name=테스트카테고리)
     * Then:  HTTP 200 + 카테고리 생성됨 / GET /api/categories → 목록에 포함
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void should_create_category_and_return_in_list() {
        // When — 카테고리 생성
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/categories?name={name}",
            null,
            Map.class,
            "테스트카테고리"
        );

        // Then — 생성 성공 (HTTP 200)
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).containsKey("id");

        // Then — 후속 행동 검증: GET /api/categories 에서 목록에 포함
        ResponseEntity<List> listResponse = restTemplate.getForEntity(
            "/api/categories",
            List.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(1);
    }
}
