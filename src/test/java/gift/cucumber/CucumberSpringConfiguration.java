package gift.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    @Value("${test.port:0}")
    private int testPort;

    public int getPort() {
        return testPort > 0 ? testPort : port;
    }
}
