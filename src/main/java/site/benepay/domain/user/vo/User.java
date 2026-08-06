package site.benepay.domain.user.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * users 테이블의 한 행. 필드 순서는 스키마의 컬럼 순서를 따른다.
 *
 * <p>생성 시점에 확정되는 식별·감사 컬럼에는 setter를 두지 않는다. 생성 이후 애플리케이션이
 * 실제로 갱신하는 컬럼(phoneNumber, pinHash, deleted - UserMapper.xml 참고)에만 필드 단위로
 * @Setter를 붙였다.
 */
@Getter
@NoArgsConstructor // MyBatis가 리플렉션으로 필드를 채우려면 기본 생성자가 필요하다
@AllArgsConstructor // @Builder가 내부적으로 사용
@Builder
public class User {

	private Long userId;
	private String loginId;
	private String loginPasswordHash;
	@Setter
	private String pinHash;
	private String name;
	@Setter
	private String phoneNumber;
	// 컬럼이 CHAR(8) 'YYYYMMDD'이다. LocalDate로 받으면 TypeHandler를 따로 붙여야 하는데,
	// 날짜 연산을 하는 곳이 없어 저장 형식 그대로 두는 편이 단순하다.
	private String birthDate;
	private Role role;
	private String di;
	private String ciHash;
	private String ciEncrypted;
	private LocalDateTime createdAt;
	@Setter
	private boolean deleted;
	private String fcmToken;
}
