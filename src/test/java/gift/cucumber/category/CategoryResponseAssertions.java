package gift.cucumber.category;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@Component
public class CategoryResponseAssertions {

    public void assertEmptyList(final Response response) {
        response.then().body("size()", equalTo(0));
    }

    public void assertListSize(final Response response, final int size) {
        response.then().body("size()", equalTo(size));
    }

    public void assertContainsCategory(final Response response, final String name) {
        response.then().body("name", hasItem(name));
    }

    public void assertCreatedName(final Response response, final String name) {
        response.then().body("name", equalTo(name));
    }
}
