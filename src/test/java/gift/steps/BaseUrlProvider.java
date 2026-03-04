package gift.steps;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class BaseUrlProvider {

    @Value("${app.base-url:}")
    private String baseUrl;

    private final Environment environment;

    public BaseUrlProvider(final Environment environment) {
        this.environment = environment;
    }

    public String getBaseUrl() {
        if (!baseUrl.isEmpty()) {
            return baseUrl;
        }
        return "http://localhost:" + environment.getProperty("local.server.port");
    }
}
