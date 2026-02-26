package gift.docker;

import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("features/gift.feature")
public class GiftCucumberDockerFeatureTest extends AbstractCucumberDockerFeatureTest {

}
