package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 원본 CI/DI는 PortOneService 밖으로 나가지 않고 가공된 형태만 실린다 (NFR-SEC-12).
 *
 * <p>필드명 ciEncrypted는 저장 컬럼 users.ci_encrypted를 따른 것이다. 실제 담기는 값은 BCrypt
 * 해시라 복호화가 되지 않으니, 원본 CI를 되찾아야 하는 요구가 생기면 이름이 아니라
 * PortOneServiceImpl의 생성 방식부터 바꿔야 한다.
 */
@Getter
@AllArgsConstructor
public class PortOneVerifyResponseDto {

    private String ciEncrypted;
    private String diHash;
}
