package site.benepay.common.exception;

/**
 * KB카드 Mock Server와의 연동 과정에서 발생하는 예외.
 */
public class KbCardIntegrationException extends RuntimeException {

	public KbCardIntegrationException(String message) {
		super(message);
	}

	public KbCardIntegrationException(String message, Throwable cause) {
		super(message, cause);
	}
}
