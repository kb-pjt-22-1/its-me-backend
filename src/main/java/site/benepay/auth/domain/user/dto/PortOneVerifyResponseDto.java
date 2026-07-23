package site.benepay.auth.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Only ever carries hashed forms of CI/DI - raw values never leave PortOneService (NFR-SEC-12).
 */
@Getter
@AllArgsConstructor
public class PortOneVerifyResponseDto {

    private String ciHash;
    private String diHash;
}
