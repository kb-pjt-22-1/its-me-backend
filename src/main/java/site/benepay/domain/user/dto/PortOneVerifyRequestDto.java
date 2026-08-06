package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실제 PortOne 연동에서는 name/phoneNumber/birthDate가 PortOne 자체 인증 화면에서
 * 입력·검증돼 서버-to-서버 조회 응답으로 돌아온다(NFR 참고: unique_key/unique_in_site와
 * 함께 name/phone/birthday로 내려옴). 지금은 PortOne 키가 없어 그 화면을 대신하는 모달을
 * 프론트에 두고, 거기서 받은 값을 그대로 여기로 실어 보낸다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortOneVerifyRequestDto {

	@NotBlank
	private String impUid;

	@NotBlank
	@Size(max = 50)
	private String name;

	@Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "invalid phone number format")
	private String phoneNumber;

	@Pattern(regexp = "^\\d{8}$", message = "birthDate must be 8 digits in YYYYMMDD format")
	private String birthDate;
}
