package site.benepay.integration.kbcard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KB카드 Mock Server에 "이 사람이 실명 등록된 KB 회원인지" 확인을 요청할 때 보내는 바디.
 * 이름/생년월일/휴대폰번호는 원문 개인정보라, ciHash(카드 조회용)처럼 쿼리스트링에 실어
 * URL·서버 로그에 남기지 않도록 GET+쿼리 대신 POST+바디로 보낸다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KbCustomerVerifyRequestDto {

	private String name;
	private String birthDate;
	private String phoneNumber;
}
