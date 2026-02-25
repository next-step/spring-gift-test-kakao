package gift.cucumber;

import gift.DatabaseCleaner;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;

public class CucumberHooks {

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Before
    public void setUp() {
        RestAssured.port = 28080;
        databaseCleaner.clear();
    }
}
