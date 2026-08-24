package site.benepay.common.exception;

/**
 * 입력한 이름/생년월일/휴대폰번호로 KB Mock Server에 등록된 실명 회원을 찾지 못했을 때.
 * 내부 중복 가입(DuplicateUserException)과는 반대 의미라 별도 예외로 분리한다 -
 * 하나로 뭉뚱그리면 "이미 benepay 회원"과 "애초에 KB 회원이 아님"을 프론트가 구분해
 * 안내할 수 없다.
 */
public class KbCustomerNotFoundException extends RuntimeException {

	public KbCustomerNotFoundException(String message) {
		super(message);
	}
}
