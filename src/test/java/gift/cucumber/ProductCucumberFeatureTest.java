package gift.cucumber;

import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("features/product.feature")
public class ProductCucumberFeatureTest extends AbstractCucumberFeatureTest {

}
