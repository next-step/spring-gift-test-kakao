package gift.cucumber;

import gift.DatabaseCleaner;
import io.cucumber.java.After;
import org.springframework.beans.factory.annotation.Autowired;

public class CommonStepDefinitions {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @After
    public void cleanDatabase() {
        databaseCleaner.clear();
    }
}
