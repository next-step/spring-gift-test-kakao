package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;

public class RestAssuredConfigHook {

    @LocalServerPort
    int port;

    @Value("${test.server.url:}")
    String serverUrl;

    @Before(order = 0)
    public void configureRestAssured() {
        if (!serverUrl.isEmpty()) {
            RestAssured.baseURI = serverUrl;
            RestAssured.port = RestAssured.UNDEFINED_PORT;
        } else {
            RestAssured.baseURI = "http://localhost";
            RestAssured.port = port;
        }
    }
}
