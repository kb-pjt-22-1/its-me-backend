package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * (테스트용) 이 프로젝트는 실제 본인인증기관/SMS 게이트웨이 연동이 없는 목데이터 전용
 * 서비스라, devVerificationCode에 실제 인증번호를 그대로 실어 준다 - 그래야 실제 문자
 * 없이도 회원가입 2단계(인증 확인)를 테스트할 수 있다.
 */
@Getter
@AllArgsConstructor
public class SignupIdentityRequestResponseDto {

	private String devVerificationCode;
}
