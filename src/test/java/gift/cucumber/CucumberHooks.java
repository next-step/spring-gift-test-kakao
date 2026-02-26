package gift.cucumber;

import gift.TestDataManipulator;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Cucumber 시나리오 생명주기 훅.
 * <p>
 * {@code @Before}: RestAssured 포트 및 로깅 설정 (기존 {@code AcceptanceTestSupport.setupRestAssured()} 대응)
 * {@code @After}: 테스트 데이터 정리 (기존 {@code @AfterEach initAll()} 대응)
 */
public class CucumberHooks {

    private final TestDataManipulator dataManipulator;

    private final int port;

    public CucumberHooks(
            TestDataManipulator dataManipulator,
            @LocalServerPort int port
    ) {
        this.dataManipulator = dataManipulator;
        this.port = port;
    }

    @Before(order = 0)
    public void setUp() {
        RestAssured.reset();
        RestAssured.port = port;
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @After(order = 0)
    public void cleanUp() {
        dataManipulator.initAll();
    }
}
