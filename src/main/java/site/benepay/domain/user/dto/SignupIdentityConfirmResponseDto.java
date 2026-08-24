package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증번호 검증까지 끝난 신원 정보(이름·전화번호·생년월일·CI·DI)는 서버에만 남고, 프론트에는
 * 그 값을 담고 있는 짧은 수명의 1회용 토큰만 나간다. /signup 호출 시 이 토큰을 그대로 다시
 * 보내면 된다 - SignupVerificationStore가 발급하는 값과 같은 성격(NFR-SEC-12).
 */
@Getter
@AllArgsConstructor
public class SignupIdentityConfirmResponseDto {

	private String verificationToken;
}
