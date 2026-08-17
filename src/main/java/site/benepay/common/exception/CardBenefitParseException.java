package site.benepay.common.exception;

/**
 * 카드 혜택 정보 파싱에 실패했을 때 발생하는 예외.
 */
public class CardBenefitParseException extends RuntimeException {

	public CardBenefitParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
