package site.benepay.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.mindrot.jbcrypt.BCrypt;

public final class DatabaseInitializer {
	private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

	private DatabaseInitializer() {
	}

	public static void initialize() {
		try (Connection connection = Database.getConnection()) {
			connection.setAutoCommit(false);
			try {
				String script = Database.getUrl().startsWith("jdbc:mysql:") ? "schema-mysql.sql" : "schema-h2.sql";
				executeScript(connection, script);
				seedDemoData(connection);
				ensureDemoPayments(connection);
				ensureDemoPin(connection);
				connection.commit();
				System.out.println("[BenePay] Database initialized: " + Database.getUrl());
			} catch (Exception e) {
				connection.rollback();
				throw e;
			}
		} catch (Exception e) {
			throw new IllegalStateException("BenePay database initialization failed", e);
		}
	}

	private static void executeScript(Connection connection, String resourceName) throws Exception {
		InputStream input = DatabaseInitializer.class.getClassLoader().getResourceAsStream(resourceName);
		if (input == null)
			throw new IllegalStateException("SQL resource not found: " + resourceName);
		StringBuilder sql = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#"))
					continue;
				sql.append(line).append('\n');
			}
		}
		try (Statement statement = connection.createStatement()) {
			for (String command : sql.toString().split(";")) {
				if (!command.trim().isEmpty())
					statement.execute(command.trim());
			}
		}
	}

	private static void seedDemoData(Connection connection) throws Exception {
		if (count(connection, "users") > 0)
			return;

		execute(connection, "INSERT INTO merchant_categories (category_id, category_code, category_name) VALUES " +
			"(1, 'FOOD', '카페/음식점'), (2, 'MART', '마트/편의점'), (3, 'CULTURE', '문화/여가'), " +
			"(4, 'TRANSPORT', '교통'), (5, 'SHOPPING', '쇼핑'), (6, 'MEDICAL', '병원/약국'), (7, 'FUEL', '주유소')");

		execute(connection, "INSERT INTO cards " +
			"(card_id, card_name, card_type, description, is_supported, card_company_name, min_benefit_amount) VALUES "
			+
			"(1, 'Deep Dream Platinum', 'CREDIT', '카페와 음식점 혜택을 제공하는 대표 카드', TRUE, '신한카드', 300000), " +
			"(2, 'Shinhan The More', 'CREDIT', '전 가맹점 포인트 적립 카드', TRUE, '신한카드', 0), " +
			"(3, '매일 캐시백 체크', 'CHECK', '실적 조건 없는 기본 캐시백 카드', TRUE, 'BenePay', 0)");

		String passwordHash = BCrypt.hashpw("demo1234", BCrypt.gensalt(10));
		String pinHash = BCrypt.hashpw("123456", BCrypt.gensalt(10));
		execute(connection, "INSERT INTO users " +
				"(user_id, email, login_id, password_hash, pin_hash, pin_fail_count, name, phone_number, role, di, created_at, is_deleted) "
				+
				"VALUES (1, 'point@example.com', 'point', ?, ?, 0, '김포인트', '010-1234-5678', 'USER', " +
				"'BENEPOINT-DEMO-1', CURRENT_TIMESTAMP, FALSE)",
			passwordHash, pinHash);

		execute(connection, "INSERT INTO user_cards " +
			"(user_card_id, user_id, card_id, card_token, card_last4, is_primary, recommendation_enabled, registered_at, is_deleted, annual_fee) VALUES "
			+
			"(1, 1, 1, 'TOKEN-DEEP-1234', '1234', TRUE, TRUE, CURRENT_TIMESTAMP, FALSE, 30000), " +
			"(2, 1, 2, 'TOKEN-MORE-5678', '5678', FALSE, TRUE, CURRENT_TIMESTAMP, FALSE, 15000), " +
			"(3, 1, 3, 'TOKEN-CASH-9012', '9012', FALSE, TRUE, CURRENT_TIMESTAMP, FALSE, 0)");

		insertBenefit(connection, 1, 1, "청구", "카페 10% 할인", "카페 결제 시 10% 할인",
			"{\"category\":\"CAFE\",\"discount_rate\":10,\"monthly_limit\":15000}");
		insertBenefit(connection, 2, 1, "환급", "음식점 5% 캐시백", "음식점 결제 시 5% 캐시백",
			"{\"category\":\"FOOD\",\"cashback_rate\":5,\"monthly_limit\":20000}");
		insertBenefit(connection, 3, 2, "환급", "기본 적립 3%", "전 가맹점 결제액 3% 포인트 적립",
			"{\"category\":\"ALL\",\"point_rate\":3,\"monthly_limit\":30000}");
		insertBenefit(connection, 4, 3, "환급", "기본 캐시백 0.3%", "실적 조건 없이 0.3% 캐시백",
			"{\"category\":\"ALL\",\"cashback_rate\":0.3,\"monthly_limit\":10000}");

		execute(connection, "INSERT INTO benefit_per_user_card (user_card_id, benefit_id) VALUES " +
			"(1, 1), (1, 2), (2, 3), (3, 4)");

		execute(connection, "INSERT INTO merchants " +
			"(merchant_id, category_id, merchant_code, merchant_name, brand_name, address, latitude, longitude, phone) VALUES "
			+
			"(1, 1, 'GIMPO-STARBUCKS', '스타벅스 김포점', '스타벅스', '경기도 김포시 김포대로 100', 37.6209000, 126.7198000, '031-100-0001'), "
			+
			"(2, 2, 'EMART-MALL', '이마트몰', '이마트', '경기도 김포시 풍무로 20', 37.6179000, 126.7167000, '031-100-0002'), " +
			"(3, 1, 'TODAY-COFFEE', '오늘의 커피 로스터스', '오늘의 커피', '경기도 김포시 김포한강1로 80', 37.6218000, 126.7241000, '031-100-0003'), "
			+
			"(4, 1, 'SOSOHAN-TABLE', '소소한 식탁', '소소한 식탁', '경기도 김포시 김포한강2로 150', 37.6245000, 126.7286000, '031-100-0004'), "
			+
			"(5, 2, 'CU-GIMPO', 'CU 김포한강점', 'CU', '경기도 김포시 김포한강3로 40', 37.6191000, 126.7313000, '031-100-0005'), " +
			"(6, 3, 'CGV-GIMPO', 'CGV 김포한강', 'CGV', '경기도 김포시 걸포로 6', 37.6272000, 126.7212000, '1544-1122'), " +
			"(7, 6, 'GOOD-HOSPITAL', '김포 좋은병원', '좋은병원', '경기도 김포시 돌문로 30', 37.6158000, 126.7238000, '031-100-0007'), " +
			"(8, 7, 'HAPPY-FUEL', '행복 주유소', '행복주유소', '경기도 김포시 양촌로 77', 37.6259000, 126.7161000, '031-100-0008')");

		String currentMonth = YearMonth.now().format(YEAR_MONTH);
		execute(connection, "INSERT INTO card_monthly_status " +
				"(user_card_id, total_spending_amount, is_benefit_eligible, updated_at, target_year_month) VALUES " +
				"(1, 255000, TRUE, CURRENT_TIMESTAMP, ?), (2, 180000, TRUE, CURRENT_TIMESTAMP, ?), " +
				"(3, 82000, TRUE, CURRENT_TIMESTAMP, ?)",
			currentMonth, currentMonth, currentMonth);

		insertHistory(connection, 1, 1, LocalDateTime.now().minusDays(2), 6000, 600, 0, 5400,
			"APPROVED", "카페 10% 할인");
		insertHistory(connection, 2, 2, LocalDateTime.now().minusDays(3), 42800, 0, 2140, 42800,
			"APPROVED", "기본 포인트 적립");
		insertHistory(connection, 1, 3, LocalDateTime.now().minusDays(8), 12000, 1200, 0, 10800,
			"APPROVED", "카페 10% 할인");

		execute(connection,
			"INSERT INTO bookmarked_stores (user_id, merchant_id, created_at, is_deleted) VALUES (?, ?, ?, FALSE)",
			1L, 4L, Timestamp.valueOf(LocalDateTime.now().minusSeconds(1)));
		execute(connection,
			"INSERT INTO bookmarked_stores (user_id, merchant_id, created_at, is_deleted) VALUES (?, ?, ?, FALSE)",
			1L, 3L, Timestamp.valueOf(LocalDateTime.now()));
	}

	private static void ensureDemoPin(Connection connection) throws Exception {
		if (!AppConfig.getBoolean("demo.mode", true) || count(connection, "users") == 0)
			return;
		String pinHash = BCrypt.hashpw("123456", BCrypt.gensalt(10));
		execute(connection,
			"UPDATE users SET pin_hash = ?, pin_fail_count = 0, pin_locked_until = NULL WHERE user_id = 1", pinHash);
	}

	private static void insertBenefit(Connection connection, long benefitId, long cardId, String type,
		String name, String description, String info) throws Exception {
		execute(connection, "INSERT INTO card_benefits " +
				"(benefit_id, card_id, benefit_type, benefit_name, description, benefits_info) VALUES (?, ?, ?, ?, ?, ?)",
			benefitId, cardId, type, name, description, info);
	}

	private static void insertHistory(Connection connection, long userCardId, long merchantId,
		LocalDateTime paymentTime, long original, long discount, long reward,
		long finalAmount, String status, String description) throws Exception {
		execute(connection, "INSERT INTO payments " +
				"(user_card_id, merchant_id, payment_time, original_amount, discount_amount, reward_amount, " +
				"final_amount, payment_status, benefit_description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
			userCardId, merchantId, Timestamp.valueOf(paymentTime), original, discount, reward,
			finalAmount, status, description);
	}

	private static long count(Connection connection, String table) throws Exception {
		try (Statement statement = connection.createStatement();
			 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private static void execute(Connection connection, String sql, Object... params) throws Exception {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++)
				ps.setObject(i + 1, params[i]);
			ps.executeUpdate();
		}
	}
	/**
	 * 사용자 데이터는 존재하지만 결제 목업 데이터가 없는 경우
	 * 결제 내역만 별도로 생성한다.
	 */
	private static void ensureDemoPayments(
		Connection connection
	) throws Exception {

		/*
		 * 이미 결제 데이터가 있으면 중복 생성하지 않는다.
		 */
		if (count(connection, "payments") > 0) {
			return;
		}

		insertHistory(
			connection,
			1,
			1,
			LocalDateTime.now().minusDays(2),
			6000,
			600,
			0,
			5400,
			"APPROVED",
			"카페 10% 할인"
		);

		insertHistory(
			connection,
			2,
			2,
			LocalDateTime.now().minusDays(3),
			42800,
			0,
			2140,
			42800,
			"APPROVED",
			"기본 포인트 적립"
		);

		insertHistory(
			connection,
			1,
			3,
			LocalDateTime.now().minusDays(8),
			12000,
			1200,
			0,
			10800,
			"APPROVED",
			"카페 10% 할인"
		);

		System.out.println(
			"[BenePay] Demo payment history inserted."
		);
	}
}
