package gift.acceptance.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.not;

public class CommonStepDefs {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SharedContext context;

    @Before
    public void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE wish");
        jdbcTemplate.execute("TRUNCATE TABLE option");
        jdbcTemplate.execute("TRUNCATE TABLE product");
        jdbcTemplate.execute("TRUNCATE TABLE category");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @먼저("id {int}번의 {string} 회원이 존재한다")
    public void 회원이_존재한다(int id, String name) {
        jdbcTemplate.update("INSERT INTO member (id, name, email) VALUES (?, ?, ?)", id, name, name + "@test.com");
    }

    @먼저("id {int}번의 {string} 카테고리가 존재한다")
    public void 카테고리가_존재한다(int id, String name) {
        jdbcTemplate.update("INSERT INTO category (id, name) VALUES (?, ?)", id, name);
    }

    @먼저("카테고리 {int}번에 id {int}번의 {string} 상품이 존재한다")
    public void 카테고리에_상품이_존재한다(int categoryId, int productId, String productName) {
        jdbcTemplate.update(
            "INSERT INTO product (id, name, price, image_url, category_id) VALUES (?, ?, 10000, 'http://test.com/image.jpg', ?)",
            productId, productName, categoryId);
    }

    @먼저("상품 {int}번에 재고 {int}개짜리 id {int}번의 {string} 옵션이 존재한다")
    public void 상품에_옵션이_존재한다(int productId, int quantity, int optionId, String optionName) {
        jdbcTemplate.update(
            "INSERT INTO option (id, name, quantity, product_id) VALUES (?, ?, ?, ?)",
            optionId, optionName, quantity, productId);
    }

    @그러면("응답 코드는 {int}이다")
    public void 응답_코드는_이다(int statusCode) {
        context.getResponse().then().statusCode(statusCode);
    }

    @그러면("응답 코드는 {int}이 아니다")
    public void 응답_코드는_이_아니다(int statusCode) {
        context.getResponse().then().statusCode(not(statusCode));
    }

    @그리고("응답에 {string} 이름이 포함되어 있다")
    public void 응답에_이름이_포함되어_있다(String name) {
        context.getResponse().then().body("name", org.hamcrest.Matchers.hasItem(name));
    }
}
