package site.benepay.common.exception;

// 존재하지 않거나, 요청한 사용자 소유가 아니거나, 비활성/만료된 카드로 결제 토큰을 발급하려 할 때.
public class UserCardNotAvailableException extends RuntimeException {

	public UserCardNotAvailableException(String message) {
		super(message);
	}
}
