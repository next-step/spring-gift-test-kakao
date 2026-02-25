package gift;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

public class SharedContext {

    static final String BASE_URL = "http://localhost:18080";
    static final String DB_URL = "jdbc:postgresql://localhost:5432/testdb";
    static final String DB_USER = "postgres";
    static final String DB_PASSWORD = "postgres";

    private static ExtractableResponse<Response> response;
    private static final Map<String, Long> categoryIds = new HashMap<>();
    private static final Map<String, Long> memberIds = new HashMap<>();
    private static final Map<String, Long> optionIds = new HashMap<>();

    public static void clear() {
        response = null;
        categoryIds.clear();
        memberIds.clear();
        optionIds.clear();
    }

    public static ExtractableResponse<Response> getResponse() {
        return response;
    }

    public static void setResponse(ExtractableResponse<Response> resp) {
        response = resp;
    }

    public static void putCategoryId(String name, Long id) {
        categoryIds.put(name, id);
    }

    public static Long getCategoryId(String name) {
        return categoryIds.get(name);
    }

    public static void putMemberId(String name, Long id) {
        memberIds.put(name, id);
    }

    public static Long getMemberId(String name) {
        return memberIds.get(name);
    }

    public static void putOptionId(String name, Long id) {
        optionIds.put(name, id);
    }

    public static Long getOptionId(String name) {
        return optionIds.get(name);
    }
}
