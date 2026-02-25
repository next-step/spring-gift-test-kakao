package gift.support;

import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Component
public class ApiClient {
	private final Environment environment;

	public ApiClient(Environment environment) {
		this.environment = environment;
	}

	private int getPort() {
		return Integer.parseInt(environment.getProperty("test.server.port", "28080"));
	}

	public Response sendGift(Long senderId, Long receiverId, Long optionId, int quantity) {
		return RestAssured
			.given().log().all()
			.port(getPort())
			.contentType(ContentType.JSON)
			.header("Member-Id", senderId)
			.body(Map.of(
				"optionId", optionId,
				"quantity", quantity,
				"receiverId", receiverId,
				"message", "생일 축하해!"
			))
			.when()
			.post("/api/gifts");
	}

	public Response createCategory(String name) {
		return RestAssured
			.given().log().all()
			.port(getPort())
			.contentType(ContentType.JSON)
			.body(Map.of("name", name))
			.when()
			.post("/api/categories");
	}

	public Response getCategories() {
		return RestAssured
			.given().log().all()
			.port(getPort())
			.when()
			.get("/api/categories");
	}

	public Response createProduct(String name, int price, Long categoryId) {
		return RestAssured
			.given().log().all()
			.port(getPort())
			.contentType(ContentType.JSON)
			.body(Map.of(
				"name", name,
				"price", price,
				"imageUrl", "https://example.com/image.png",
				"categoryId", categoryId
			))
			.when()
			.post("/api/products");
	}

	public Response getProducts() {
		return RestAssured
			.given().log().all()
			.port(getPort())
			.when()
			.get("/api/products");
	}
}
