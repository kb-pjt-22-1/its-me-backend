package site.benepay.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import site.benepay.config.AppConfig;
import site.benepay.service.ServiceException;

public final class SessionAuthUtil {
	public static final String PIN_VERIFIED_UNTIL = "PIN_VERIFIED_UNTIL";
	public static final String LATEST_PAYMENT_TOKEN = "LATEST_PAYMENT_TOKEN";

	private SessionAuthUtil() {
	}

	public static void markPinVerified(HttpServletRequest request) {
		int seconds = AppConfig.getInt("pin.sessionSeconds", 120);
		request.getSession(true).setAttribute(PIN_VERIFIED_UNTIL, System.currentTimeMillis() + seconds * 1000L);
	}

	public static void requirePinVerified(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new ServiceException(401, "PIN_AUTH_REQUIRED", "결제 코드 발급 전에 PIN 인증이 필요합니다.");
		}
		Object value = session.getAttribute(PIN_VERIFIED_UNTIL);
		if (!(value instanceof Long) || ((Long)value) < System.currentTimeMillis()) {
			session.removeAttribute(PIN_VERIFIED_UNTIL);
			throw new ServiceException(401, "PIN_AUTH_EXPIRED", "PIN 인증 시간이 만료되었습니다. 다시 인증하세요.");
		}
	}
}
