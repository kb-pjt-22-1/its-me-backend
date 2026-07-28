package site.benepay.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
	private static final Properties PROPERTIES = new Properties();

	static {
		try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
			if (input != null) {
				PROPERTIES.load(input);
			}
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private AppConfig() {
	}

	public static String get(String key, String defaultValue) {
		String envKey = "BENEPAY_" + key.toUpperCase().replace('.', '_');
		String envValue = System.getenv(envKey);
		if (envValue != null && !envValue.trim().isEmpty()) {
			return envValue.trim();
		}
		String value = PROPERTIES.getProperty(key);
		return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
	}

	public static int getInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(get(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
	}
}
