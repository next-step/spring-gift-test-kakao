package gift.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("docker")
public class CucumberSpringConfiguration {

    @PostConstruct
    public void setUp() {
        // Docker 컨테이너의 앱에 요청
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
    }
}
