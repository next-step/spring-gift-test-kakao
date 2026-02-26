package gift.cucumber;

import gift.model.GiftDelivery;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration {

    @MockitoBean
    private GiftDelivery giftDelivery;
}
