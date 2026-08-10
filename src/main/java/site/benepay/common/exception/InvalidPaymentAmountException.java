package site.benepay.common.exception;

public class InvalidPaymentAmountException extends RuntimeException {

	public InvalidPaymentAmountException(String message) {
		super(message);
	}
}
