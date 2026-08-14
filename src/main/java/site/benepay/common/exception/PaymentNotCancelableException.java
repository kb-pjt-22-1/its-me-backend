package site.benepay.common.exception;

// 결제가 존재하지 않거나, 요청자 소유가 아니거나, 이미 APPROVED가 아닌 상태(취소 불가)일 때.
public class PaymentNotCancelableException extends RuntimeException {

	public PaymentNotCancelableException(String message) {
		super(message);
	}
}
