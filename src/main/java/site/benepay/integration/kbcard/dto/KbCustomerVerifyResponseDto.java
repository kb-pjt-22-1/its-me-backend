package site.benepay.integration.kbcard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KB카드 Mock Server의 회원 확인 응답. registered=false면 KB에 실명 등록된 회원이 아니라는
 * 뜻이고, customerReferenceId는 registered=true일 때만 채워진다 - DI(연계정보) 해시의 시드로
 * 쓴다(KbCustomerCardsResponseDto.customerReferenceId와 동일한 식별자 체계).
 */
@Getter
@Setter
@NoArgsConstructor
public class KbCustomerVerifyResponseDto {

	private boolean registered;
	private String customerReferenceId;
}
