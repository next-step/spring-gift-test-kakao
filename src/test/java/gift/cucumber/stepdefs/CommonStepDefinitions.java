package gift.cucumber.stepdefs;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

public class CommonStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private DataSource dataSource;

    @Value("${test.sql.dialect:h2}")
    private String sqlDialect;

    @Given("공통 데이터가 초기화되어 있다")
    public void 공통_데이터가_초기화되어_있다() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-data.sql"));
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/" + sqlDialect + "/reset-sequences.sql"));
        }
    }

    @Then("요청이 성공한다")
    public void 요청이_성공한다() {
        context.getResponse()
            .then()
            .statusCode(200);
    }

    @Then("요청이 실패한다")
    public void 요청이_실패한다() {
        context.getResponse()
            .then()
            .statusCode(500);
    }

    @Then("잘못된 요청으로 거부된다")
    public void 잘못된_요청으로_거부된다() {
        context.getResponse()
            .then()
            .statusCode(400);
    }
}
