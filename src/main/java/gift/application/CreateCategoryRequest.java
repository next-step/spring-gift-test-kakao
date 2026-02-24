package gift.application;


public class CreateCategoryRequest {
    private String name;

    public CreateCategoryRequest(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
