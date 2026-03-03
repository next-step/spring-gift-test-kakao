package gift;

import gift.model.GiftDelivery;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {      // 모든 Step 클래스가 공유하는 Spring 컨텍스트 설정

    @MockitoBean
    private GiftDelivery giftDelivery;
}
