package gift.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration {

    @io.cucumber.java.Before
    public void setUp() {
        RestAssured.port = 28080;
    }
}
