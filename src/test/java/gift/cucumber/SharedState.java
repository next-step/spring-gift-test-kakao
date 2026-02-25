package gift.cucumber;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
public class SharedState {

    private Response response;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public void reset() {
        this.response = null;
    }
}
