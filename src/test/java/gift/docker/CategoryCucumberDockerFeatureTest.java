package gift.docker;

import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("features/category.feature")
public class CategoryCucumberDockerFeatureTest extends AbstractCucumberDockerFeatureTest {

}
