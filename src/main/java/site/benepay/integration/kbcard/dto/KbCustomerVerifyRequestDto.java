package site.benepay.integration.kbcard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KB카드 Mock Server에 "이 사람이 실명 등록된 KB 회원인지" 확인을 요청할 때 보내는 바디.
 * 이름/생년월일/휴대폰번호 원문 대신 ciHash만 보낸다 - 해당 조합 공식(SHA-256(name+
 * birthDate+phoneNumber))은 BenePay 쪽에서만 알면 되고, Mock Server는 자신의 ci_hash
 * 컬럼과 일치하는지만 비교하면 되므로 원문 개인정보를 다시 전송할 이유가 없다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KbCustomerVerifyRequestDto {

	private String ciHash;
}
