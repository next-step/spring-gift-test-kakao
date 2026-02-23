package gift;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;

public class Fixtures {

    public static Product product(String name, Category category) {
        return new Product(name, 10000, "https://example.com/default.jpg", category);
    }

    public static Option option(int quantity, Product product) {
        return new Option("기본", quantity, product);
    }

    public static Member member(String name) {
        return new Member(name, name + "@test.com");
    }
}
