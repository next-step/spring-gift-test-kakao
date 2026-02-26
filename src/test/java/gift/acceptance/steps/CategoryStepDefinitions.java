package gift.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static gift.acceptance.steps.CommonStepDefinitions.*;

public class CategoryStepDefinitions {

    @Autowired
    private CategoryApiClient api;

    @Given("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        Long id = api.카테고리를_DB에_등록한다(name);
        categoryIds.put(name, id);
    }

    @When("{string} 카테고리를 등록한다")
    public void 카테고리를_등록한다(String name) {
        latestResponse = api.카테고리를_등록한다(name);
        latestStatusCode = latestResponse.statusCode();
    }

    @When("이름 없이 카테고리를 등록한다")
    public void 이름_없이_카테고리를_등록한다() {
        latestResponse = api.카테고리를_이름없이_등록한다();
        latestStatusCode = latestResponse.statusCode();
    }

    @When("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        latestResponse = api.카테고리_목록을_조회한다();
        latestStatusCode = latestResponse.statusCode();
    }
}
