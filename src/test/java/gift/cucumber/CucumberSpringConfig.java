package gift.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Cucumber-Spring 통합 설정.
 * <p>
 * Cucumber 시나리오 실행 시 Spring Boot 애플리케이션 컨텍스트를 부트스트랩한다. {@code RANDOM_PORT}로 실제 서버를 띄워 RestAssured
 * 기반 API 테스트를 수행한다.
 */
@SuppressWarnings("unused")
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {

}
