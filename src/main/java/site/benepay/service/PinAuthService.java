package site.benepay.service;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import site.benepay.config.AppConfig;
import site.benepay.config.Database;
import site.benepay.repository.UserRepository;

public class PinAuthService {
	private final UserRepository userRepository = new UserRepository();
	private final int maxFailures = AppConfig.getInt("pin.maxFailures", 5);
	private final int lockSeconds = AppConfig.getInt("pin.lockSeconds", 300);

	public Map<String, Object> verify(long userId, String pin) {
		if (pin == null || !pin.matches("\\d{6}")) {
			throw new ServiceException(400, "INVALID_PIN_FORMAT", "PIN은 숫자 6자리로 입력해야 합니다.");
		}

		try (Connection connection = Database.getConnection()) {
			connection.setAutoCommit(false);
			try {
				LocalDateTime lockedUntil = userRepository.findPinLockedUntil(connection, userId);
				LocalDateTime now = LocalDateTime.now();
				if (lockedUntil != null && lockedUntil.isAfter(now)) {
					connection.rollback();
					throw new ServiceException(423, "PIN_LOCKED", "PIN 인증이 잠겨 있습니다. 잠시 후 다시 시도하세요.");
				}

				String hash = userRepository.findPinHash(connection, userId);
				if (hash == null) {
					connection.rollback();
					throw new ServiceException(404, "PIN_NOT_REGISTERED", "등록된 PIN이 없습니다.");
				}

				if (BCrypt.checkpw(pin, hash)) {
					userRepository.resetPinFailures(connection, userId);
					connection.commit();
					Map<String, Object> result = new LinkedHashMap<String, Object>();
					result.put("verified", true);
					result.put("message", "PIN 인증에 성공했습니다.");
					result.put("validForSeconds", AppConfig.getInt("pin.sessionSeconds", 120));
					return result;
				}

				int failCount = userRepository.findPinFailCount(connection, userId) + 1;
				LocalDateTime newLockedUntil = failCount >= maxFailures ? now.plusSeconds(lockSeconds) : null;
				userRepository.recordPinFailure(connection, userId, failCount, newLockedUntil);
				connection.commit();

				int remaining = Math.max(0, maxFailures - failCount);
				if (newLockedUntil != null) {
					throw new ServiceException(423, "PIN_LOCKED", "PIN을 " + maxFailures + "회 잘못 입력하여 잠금 처리되었습니다.");
				}
				throw new ServiceException(401, "PIN_MISMATCH", "PIN이 일치하지 않습니다. 남은 횟수: " + remaining + "회");
			} catch (ServiceException e) {
				throw e;
			} catch (Exception e) {
				connection.rollback();
				throw e;
			}
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(500, "PIN_VERIFY_FAILED", "PIN 인증 처리 중 오류가 발생했습니다.", e);
		}
	}
}
