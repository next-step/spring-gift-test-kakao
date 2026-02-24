package gift.cucumber.steps.product;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@Component
public class ProductResponseAssertions {

    public void assertEmptyList(final Response response) {
        response.then().body("size()", equalTo(0));
    }

    public void assertListSize(final Response response, final int size) {
        response.then().body("size()", equalTo(size));
    }

    public void assertContainsProductName(final Response response, final String name) {
        response.then().body("name", hasItem(name));
    }

    public void assertProductFields(final Response response, final String name, final int price, final String imageUrl, final String categoryName) {
        response.then()
            .body("name", equalTo(name))
            .body("price", equalTo(price))
            .body("imageUrl", equalTo(imageUrl))
            .body("category.name", equalTo(categoryName));
    }

    public void assertListContainsProductAt(final Response response, final int index, final String name, final int price, final String imageUrl, final String categoryName) {
        response.then()
            .body("[" + index + "].name", equalTo(name))
            .body("[" + index + "].price", equalTo(price))
            .body("[" + index + "].imageUrl", equalTo(imageUrl))
            .body("[" + index + "].category.name", equalTo(categoryName));
    }
}
