package site.benepay.common.exception;

/**
 * 요청한 카드가 사용자 소유가 아니거나,
 * 정상적으로 사용할 수 없는 상태일 때 발생하는 예외.
 */
public class UserCardNotAvailableException extends RuntimeException {

	public UserCardNotAvailableException(String message) {
		super(message);
	}
}
