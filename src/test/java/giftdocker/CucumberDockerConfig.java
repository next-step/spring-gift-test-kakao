package giftdocker;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest(classes = gift.Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(CucumberDockerConfig.StepsScanConfig.class)
public class CucumberDockerConfig {

    @TestConfiguration
    @ComponentScan("steps")
    static class StepsScanConfig {
    }
}
