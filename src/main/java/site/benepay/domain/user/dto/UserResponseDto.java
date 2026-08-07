package site.benepay.domain.user.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;

/**
 * 본인에게 돌려주는 프로필. di/ciEncrypted/pinHash처럼 인증에 쓰이는 값과, 프로필이 아니라
 * 기기에 딸린 값인 fcmToken은 담지 않는다. pinHash 자체는 안 담지만, PIN 등록 여부는
 * 프론트가 "초기 설정" 화면과 "PIN 변경" 화면 중 뭘 보여줄지 판단해야 해서 boolean으로만 노출한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserResponseDto {

	private Long userId;
	private String loginId;
	private String name;
	private String phoneNumber;
	private String birthDate;
	private Role role;
	private LocalDateTime createdAt;
	private boolean pinRegistered;

	public static UserResponseDto from(User user) {
		return UserResponseDto.builder()
			.userId(user.getUserId())
			.loginId(user.getLoginId())
			.name(user.getName())
			.phoneNumber(user.getPhoneNumber())
			.birthDate(user.getBirthDate())
			.role(user.getRole())
			.createdAt(user.getCreatedAt())
			.pinRegistered(user.getPinHash() != null)
			.build();
	}
}
