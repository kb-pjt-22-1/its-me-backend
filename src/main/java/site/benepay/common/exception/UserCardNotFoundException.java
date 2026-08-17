package site.benepay.common.exception;

/**
 * 요청한 사용자의 보유 카드를 찾을 수 없을 때 발생하는 예외.
 */
public class UserCardNotFoundException extends RuntimeException {

	public UserCardNotFoundException(String message) {
		super(message);
	}
}
