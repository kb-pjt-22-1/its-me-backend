package site.benepay.common.exception;

/**
 * 카드 실적 갱신에 실패했을 때 발생하는 예외
 */
public class CardPerformanceUpdateException extends RuntimeException {

	public CardPerformanceUpdateException() {
		super("카드 실적 갱신에 실패했습니다.");
	}

	public CardPerformanceUpdateException(String message) {
		super(message);
	}
}
