package gift;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CucumberSpringConfig.TestcontainersConfig.class)
@ComponentScan("steps")
public class CucumberSpringConfig {

    @TestConfiguration(proxyBeanMethods = false)
    @Profile("cucumber-alone")
    static class TestcontainersConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>("postgres:16");
        }
    }
}
