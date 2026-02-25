package gift.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.조건;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CommonStepDefinitions {

    @Autowired
    private CucumberSpringConfiguration configuration;

    @Autowired
    private SharedState sharedState;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        cleanUpDatabase();
        configuration.setUpRestAssured();
        sharedState.reset();
    }

    private void cleanUpDatabase() {
        jdbcTemplate.execute("DELETE FROM wish");
        jdbcTemplate.execute("DELETE FROM option");
        jdbcTemplate.execute("DELETE FROM product");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM member");
    }

    @조건("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String categoryName) {
        jdbcTemplate.update("INSERT INTO category (name) VALUES (?)", categoryName);
    }

    @그러면("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드는_이다(int statusCode) {
        sharedState.getResponse().then().statusCode(statusCode);
    }
}
