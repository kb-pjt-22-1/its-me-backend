package site.benepay.domain.benefit.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 가장 최근 승인된 결제 1건의 가맹점 위경도. "최근 결제한 곳 주변에서 받을 수 있는 혜택"
 * (#48 2번 섹션) 전용.
 */
@Getter
@Setter
public class RecentPaymentLocationVO {

	private BigDecimal latitude;
	private BigDecimal longitude;
}
