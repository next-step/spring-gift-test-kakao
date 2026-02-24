package gift.acceptance;

import gift.support.DatabaseCleaner;
import gift.support.TestDataBuilder;
import io.cucumber.java.Before;

public class CommonHooks {

	private final DatabaseCleaner databaseCleaner;
	private final TestDataBuilder testDataBuilder;

	public CommonHooks(DatabaseCleaner databaseCleaner, TestDataBuilder testDataBuilder) {
		this.databaseCleaner = databaseCleaner;
		this.testDataBuilder = testDataBuilder;
	}

	@Before
	public void setUp() {
		databaseCleaner.clean();
		testDataBuilder.reset();
	}
}
