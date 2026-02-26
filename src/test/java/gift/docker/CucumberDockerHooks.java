package gift.docker;

import gift.TestDataManipulator;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;

public class CucumberDockerHooks {

    private static final int port = 28080;
    private final TestDataManipulator dataManipulator;

    public CucumberDockerHooks(TestDataManipulator dataManipulator) {
        this.dataManipulator = dataManipulator;
    }

    @Before(order = 0)
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @After(order = 0)
    public void cleanUp() {
        dataManipulator.initAll();
    }
}
