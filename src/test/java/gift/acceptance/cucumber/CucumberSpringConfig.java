package gift.acceptance.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("cucumber")
public class CucumberSpringConfig {

    @PostConstruct
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
    }
}
