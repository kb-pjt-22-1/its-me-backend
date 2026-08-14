package site.benepay.common.exception;

/**
 * KB카드 Webhook의 CI 해시에 해당하는
 * BenePay 사용자를 찾을 수 없을 때 발생하는 예외.
 */
public class KbCardWebhookUserNotFoundException extends RuntimeException {

	public KbCardWebhookUserNotFoundException(String message) {
		super(message);
	}
}
