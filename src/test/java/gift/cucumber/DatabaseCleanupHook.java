package gift.cucumber;

import gift.support.DatabaseCleanup;
import io.cucumber.java.Before;

public class DatabaseCleanupHook {

    private final DatabaseCleanup databaseCleanup;

    public DatabaseCleanupHook(DatabaseCleanup databaseCleanup) {
        this.databaseCleanup = databaseCleanup;
    }

    @Before
    public void cleanDatabase() {
        databaseCleanup.execute();
    }
}
