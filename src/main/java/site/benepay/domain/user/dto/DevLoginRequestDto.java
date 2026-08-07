package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개발용 자동 로그인 요청.
 *
 * <p>계정 이름 대신 슬롯 번호만 받는다. 이 엔드포인트로는 dev{n} 외의 어떤 계정도 지목할 수
 * 없으므로, 설령 실수로 켜진 채 배포되더라도 실제 사용자 계정에는 닿지 못한다.
 *
 * <p>슬롯을 여러 개 두는 이유는 refresh 세션이 userId당 하나(refresh:{userId})이기 때문이다.
 * 여러 대가 같은 계정으로 들어오면 나중에 로그인한 쪽이 세션을 덮어쓰고, 먼저 들어온 쪽이
 * 토큰을 갱신할 때 JTI가 어긋나 탈취로 오인돼 양쪽 다 로그아웃된다. 컴퓨터마다 다른 슬롯을 쓴다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DevLoginRequestDto {

	private Integer slot;
}
