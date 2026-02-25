package gift;

import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonStepDefinitions {

    @Before
    public void setUp() throws Exception {
        SharedContext.clear();
        try (Connection conn = DriverManager.getConnection(
                SharedContext.DB_URL, SharedContext.DB_USER, SharedContext.DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM wish");
            stmt.execute("DELETE FROM option");
            stmt.execute("DELETE FROM product");
            stmt.execute("DELETE FROM category");
            stmt.execute("DELETE FROM member");
        }
    }

    @그러면("성공한다")
    public void 성공한다() {
        assertThat(SharedContext.getResponse().statusCode()).isBetween(200, 299);
    }

    @그러면("실패한다")
    public void 실패한다() {
        assertThat(SharedContext.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그리고("응답의 {string}은 {string}이다")
    public void 응답_문자열_필드_확인(String field, String expected) {
        assertThat(SharedContext.getResponse().jsonPath().getString(field)).isEqualTo(expected);
    }

    @그리고("응답의 정수 {string}는 {int}이다")
    public void 응답_정수_필드_확인(String field, int expected) {
        assertThat(SharedContext.getResponse().jsonPath().getInt(field)).isEqualTo(expected);
    }

    @그리고("응답 목록에 {string}이 {string}, {string}을 포함한다")
    public void 응답_목록_포함_확인(String field, String value1, String value2) {
        assertThat(SharedContext.getResponse().jsonPath().getList(field)).contains(value1, value2);
    }

    @그리고("응답 목록이 비어있다")
    public void 응답_목록_비어있음() {
        assertThat(SharedContext.getResponse().jsonPath().getList("$")).isEmpty();
    }
}
