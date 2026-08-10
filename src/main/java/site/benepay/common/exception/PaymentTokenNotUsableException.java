package site.benepay.common.exception;

// 토큰은 존재하지만(만료 전) 이미 USED 등 ISSUED가 아닌 상태라 완료 처리할 수 없을 때.
public class PaymentTokenNotUsableException extends RuntimeException {

	public PaymentTokenNotUsableException(String message) {
		super(message);
	}
}
