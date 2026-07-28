package site.benepay.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
	private static final String URL = AppConfig.get("db.url",
		"jdbc:h2:mem:benepay;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
	private static final String USERNAME = AppConfig.get("db.username", "sa");
	private static final String PASSWORD = AppConfig.get("db.password", "");
	private static final String DRIVER = AppConfig.get("db.driver", "org.h2.Driver");

	static {
		try {
			Class.forName(DRIVER);
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError("JDBC driver load failed: " + DRIVER);
		}
	}

	private Database() {
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USERNAME, PASSWORD);
	}

	public static String getUrl() {
		return URL;
	}
}
