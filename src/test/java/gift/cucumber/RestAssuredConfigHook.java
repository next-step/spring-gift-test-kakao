package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Value;

public class RestAssuredConfigHook {

    @Value("${test.server.url}")
    String serverUrl;

    @Before(order = 0)
    public void configureRestAssured() {
        RestAssured.baseURI = serverUrl;
    }
}
