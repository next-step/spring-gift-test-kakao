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
class ProductBehaviorTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Behavior 4: 상품을 생성하면 조회 시 반환된다
     *
     * [현재 행동 — DTO 바인딩 버그]
     * CreateProductRequest에 setter가 없어 form/query 파라미터 바인딩이 실패한다.
     * categoryId가 null로 남아 findById(null) → IllegalArgumentException → HTTP 500.
     * 상품이 생성되지 않으므로 조회 시 빈 목록이 반환된다.
     *
     * Given: 카테고리(id=1)가 존재
     * When:  POST /api/products (query 파라미터: name, price, imageUrl, categoryId)
     * Then:  HTTP 500 (DTO 바인딩 실패) / GET /api/products → 빈 목록
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/product-setup.sql"})
    void should_fail_to_create_product_due_to_dto_binding_bug() {
        // When — 상품 생성 시도 (query 파라미터)
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/products?name={name}&price={price}&imageUrl={imageUrl}&categoryId={categoryId}",
            null,
            Map.class,
            "테스트상품", 10000, "http://image.url", 1
        );

        // Then — 생성 실패 (DTO에 setter 없음 → categoryId=null → findById(null) 예외)
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Then — 후속 행동 검증: 상품이 생성되지 않았으므로 빈 목록
        ResponseEntity<List> listResponse = restTemplate.getForEntity(
            "/api/products",
            List.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isEmpty();
    }

    /**
     * Behavior 5: 존재하지 않는 카테고리로 상품 생성 시 실패한다
     *
     * Given: 카테고리가 존재하지 않음 (DB 비어 있음)
     * When:  POST /api/products (query 파라미터: name, price, imageUrl, categoryId=9999)
     * Then:  HTTP 500 / GET /api/products → 빈 목록 (상품 미생성)
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void should_fail_to_create_product_when_category_does_not_exist() {
        // When — 존재하지 않는 카테고리로 상품 생성 시도
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/products?name={name}&price={price}&imageUrl={imageUrl}&categoryId={categoryId}",
            null,
            Map.class,
            "테스트상품", 10000, "http://image.url", 9999
        );

        // Then — 생성 실패
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Then — 후속 행동 검증: 상품이 생성되지 않았으므로 빈 목록
        ResponseEntity<List> listResponse = restTemplate.getForEntity(
            "/api/products",
            List.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isEmpty();
    }
}
