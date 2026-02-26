package gift.cucumber;

import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("features/gift.feature")
public class GiftCucumberFeatureTest extends AbstractCucumberFeatureTest {

}
