package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 검증된 신원 정보(이름·전화번호·생년월일·CI·DI)는 서버에만 남고, 프론트에는 그 값을 담고
 * 있는 짧은 수명의 1회용 토큰만 나간다. /signup 호출 시 이 토큰을 그대로 다시 보내면 된다.
 *
 * <p>예전에는 ciEncrypted/diHash를 여기서 직접 돌려주고 /signup이 그걸 그대로 믿었는데,
 * 그러면 이 값을 손에 넣은 누구든 본인 인증 없이 재사용해 가입할 수 있었다(NFR-SEC-12).
 */
@Getter
@AllArgsConstructor
public class PortOneVerifyResponseDto {

	private String verificationToken;
}
