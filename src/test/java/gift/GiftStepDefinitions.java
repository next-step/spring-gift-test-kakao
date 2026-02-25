package gift;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class GiftStepDefinitions {

    @먼저("회원 {string}과 {string}이 존재한다")
    public void 회원이_존재한다(String senderName, String receiverName) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                SharedContext.DB_URL, SharedContext.DB_USER, SharedContext.DB_PASSWORD)) {
            insertMember(conn, senderName);
            insertMember(conn, receiverName);
        }
    }

    private void insertMember(Connection conn, String name) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO member (name, email) VALUES (?, ?) RETURNING id")) {
            stmt.setString(1, name);
            stmt.setString(2, name + "@test.com");
            ResultSet rs = stmt.executeQuery();
            rs.next();
            SharedContext.putMemberId(name, rs.getLong("id"));
        }
    }

    @그리고("{string} 카테고리에 {int}원짜리 {string} 상품이 존재한다")
    public void 카테고리에_상품이_존재한다(String categoryName, int price, String productName) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                SharedContext.DB_URL, SharedContext.DB_USER, SharedContext.DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO category (name) VALUES (?) RETURNING id")) {
            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            Long categoryId = rs.getLong("id");
            SharedContext.putCategoryId(categoryName, categoryId);

            try (PreparedStatement prodStmt = conn.prepareStatement(
                    "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, 'http://example.com/image.png', ?)")) {
                prodStmt.setString(1, productName);
                prodStmt.setInt(2, price);
                prodStmt.setLong(3, categoryId);
                prodStmt.executeUpdate();
            }
        }
    }

    @그리고("{string}에 재고 {int}개인 {string} 옵션이 존재한다")
    public void 옵션이_존재한다(String productName, int quantity, String optionName) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                SharedContext.DB_URL, SharedContext.DB_USER, SharedContext.DB_PASSWORD);
             PreparedStatement findStmt = conn.prepareStatement(
                     "SELECT id FROM product WHERE name = ?")) {
            findStmt.setString(1, productName);
            ResultSet rs = findStmt.executeQuery();
            rs.next();
            Long productId = rs.getLong("id");

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO option (name, quantity, product_id) VALUES (?, ?, ?) RETURNING id")) {
                stmt.setString(1, optionName);
                stmt.setInt(2, quantity);
                stmt.setLong(3, productId);
                ResultSet optRs = stmt.executeQuery();
                optRs.next();
                SharedContext.putOptionId(optionName, optRs.getLong("id"));
            }
        }
    }

    @만일("{string}이 {string}에게 {string} 옵션 {int}개를 선물한다")
    public void 선물한다(String senderName, String receiverName, String optionName, int quantity) {
        Long senderId = SharedContext.getMemberId(senderName);
        Long receiverId = SharedContext.getMemberId(receiverName);
        Long optionId = SharedContext.getOptionId(optionName);
        var response = RestAssured.given().log().all()
                .baseUri(SharedContext.BASE_URL)
                .contentType("application/json")
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물"
                ))
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract();
        SharedContext.setResponse(response);
    }

    @만일("존재하지 않는 옵션으로 선물한다")
    public void 존재하지_않는_옵션으로_선물한다() {
        Long senderId = SharedContext.getMemberId("보내는사람");
        Long receiverId = SharedContext.getMemberId("받는사람");
        var response = RestAssured.given().log().all()
                .baseUri(SharedContext.BASE_URL)
                .contentType("application/json")
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", 999,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "선물"
                ))
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract();
        SharedContext.setResponse(response);
    }
}
