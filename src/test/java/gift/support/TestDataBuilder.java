package gift.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestDataBuilder {
	private final JdbcTemplate jdbcTemplate;
	private long memberIdSequence = 0;

	public TestDataBuilder(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Long createOption(String name, int quantity) {
		jdbcTemplate.update(
			"INSERT INTO category (id, name) VALUES (1, '테스트 카테고리')");
		jdbcTemplate.update(
			"INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '테스트 상품', 10000, 'http://test.com/img.png', 1)");
		jdbcTemplate.update(
			"INSERT INTO option (id, name, quantity, product_id) VALUES (1, ?, ?, 1)",
			name, quantity);
		return 1L;
	}

	public Long createMember(String name) {
		memberIdSequence++;
		jdbcTemplate.update(
			"INSERT INTO member (id, name, email) VALUES (?, ?, ?)",
			memberIdSequence, name, name + "@test.com");
		return memberIdSequence;
	}

public void reset() {
		memberIdSequence = 0;
	}
}
