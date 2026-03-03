package gift.acceptance.steps;

import gift.acceptance.DatabaseCleanup;
import gift.acceptance.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.조건;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.equalTo;

public class CommonStepDefinitions {

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScenarioContext context;

    @조건("{string} 데이터가 준비되어 있다")
    public void 데이터가_준비되어_있다(String dataName) {
        databaseCleanup.execute();

        if (!"cleanup-only".equals(dataName)) {
            String sqlFile = "/sql/" + dataName + ".sql";
            try (var stream = getClass().getResourceAsStream(sqlFile)) {
                if (stream == null) {
                    throw new IllegalArgumentException("SQL 파일을 찾을 수 없습니다: " + sqlFile);
                }
                String sql = new String(stream.readAllBytes());
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        jdbcTemplate.execute(trimmed);
                    }
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException("SQL 파일 읽기 실패: " + sqlFile, e);
            }
        }
    }

    @그러면("응답 상태코드는 {int}이다")
    public void 응답_상태코드는_이다(int statusCode) {
        context.getResponse().then().statusCode(statusCode);
    }

    @그리고("응답 에러코드는 {string}이다")
    public void 응답_에러코드는_이다(String errorCode) {
        context.getResponse().then().body("code", equalTo(errorCode));
    }
}
