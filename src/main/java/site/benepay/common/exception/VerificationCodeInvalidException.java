package site.benepay.common.exception;

/**
 * 회원가입 1단계 SMS 인증번호가 틀렸거나(발송 이력 없음 포함) 만료됐을 때.
 */
public class VerificationCodeInvalidException extends RuntimeException {

	public VerificationCodeInvalidException(String message) {
		super(message);
	}
}
