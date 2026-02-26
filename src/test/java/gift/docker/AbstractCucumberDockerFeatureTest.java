package gift.docker;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.ConfigurationParameters;
import org.junit.platform.suite.api.IncludeEngines;

@IncludeEngines("cucumber")
@ConfigurationParameters(value = {
        @ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.docker, gift.step"),
        @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
})
public abstract class AbstractCucumberDockerFeatureTest {

}
