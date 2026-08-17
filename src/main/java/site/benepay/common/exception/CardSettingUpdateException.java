package site.benepay.common.exception;

/**
 * 카드 설정 변경에 실패했을 때 발생하는 예외.
 */
public class CardSettingUpdateException extends RuntimeException {

	public CardSettingUpdateException(String message) {
		super(message);
	}
}
