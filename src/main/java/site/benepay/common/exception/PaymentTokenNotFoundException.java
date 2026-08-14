package site.benepay.common.exception;

public class PaymentTokenNotFoundException extends RuntimeException {

	public PaymentTokenNotFoundException(String message) {
		super(message);
	}
}
