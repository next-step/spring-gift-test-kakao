package gift.steps;

import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;

public class ProductStepDefs {

    @Autowired
    private ScenarioState state;

    @When("상품 목록을 조회한다")
    public void retrieveProducts() {
        state.setResponse(
                RestAssured.given()
                .when()
                        .get("/api/products")
        );
    }
}
