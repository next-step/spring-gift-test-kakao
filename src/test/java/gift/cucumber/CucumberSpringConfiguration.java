package gift.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    @Value("${test.base-url:}")
    private String baseUrl;

    @Autowired
    private Environment environment;

    public int getPort() {
        return port;
    }

    public void setUpRestAssured() {
        if (isDockerProfile()) {
            RestAssured.baseURI = baseUrl;
            RestAssured.port = RestAssured.DEFAULT_PORT;
        } else {
            RestAssured.baseURI = "http://localhost";
            RestAssured.port = port;
        }
    }

    private boolean isDockerProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("docker");
    }
}
