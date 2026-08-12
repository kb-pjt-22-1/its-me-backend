package site.benepay.common.exception;

/**
 * 혜택 조회 기간이 허용된 범위를 벗어난 경우 발생하는 예외
 */
public class InvalidBenefitPeriodException extends RuntimeException {

	public InvalidBenefitPeriodException(String message) {
		super(message);
	}
}
