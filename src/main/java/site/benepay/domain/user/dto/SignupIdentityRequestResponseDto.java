package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증번호는 응답 바디나 로그에 절대 남기지 않는다(NFR-SEC 원칙 - 인증정보를 로그에 남기지
 * 않는다는 규칙은 여기도 적용된다). 대신 dev-login과 완전히 동일한 방식으로, 이미 존재하는
 * dev-login.enabled 플래그가 켜져 있을 때만(로컬/개발 환경 전용) devVerificationCode에
 * 실제 코드를 실어 준다 - 실제 SMS 게이트웨이가 없는 지금 단계에서 로컬 테스트를 가능하게
 * 하기 위함이며, 운영 환경에서는 이 플래그가 반드시 꺼져 있어야 한다(dev-login과 동일 위험).
 */
@Getter
@AllArgsConstructor
public class SignupIdentityRequestResponseDto {

	private String devVerificationCode;
}
