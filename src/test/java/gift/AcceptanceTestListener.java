package gift;

import io.restassured.RestAssured;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

class AcceptanceTestListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        var port = testContext.getApplicationContext()
                .getEnvironment()
                .getProperty("local.server.port", Integer.class);
        RestAssured.port = port;
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        var databaseCleaner = testContext.getApplicationContext()
                .getBean(DatabaseCleaner.class);
        databaseCleaner.clear();
    }
}
