package gift.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({"e2e", "test"})
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class Config {

        @Bean
        @ScenarioScope
        public ScenarioContext scenarioContext() {
            return new ScenarioContext();
        }

        @Bean
        @ScenarioScope
        public ApiClient apiClient(@Value("${test.server.port}") int port) {
            return new ApiClient(port);
        }
    }
}
