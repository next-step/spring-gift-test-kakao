package gift.docker;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SuppressWarnings("unused")
@CucumberContextConfiguration
@ActiveProfiles("docker-test")
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class CucumberDockerSpringConfig {

}
