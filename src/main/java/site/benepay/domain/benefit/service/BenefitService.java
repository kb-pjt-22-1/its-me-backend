package site.benepay.domain.benefit.service;

import java.util.List;

import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;

public interface BenefitService {

	/**
	 * 사용자가 보유한 카드들의 연회비 본전 현황을 조회한다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param year 조회 기준 연도
	 * @return 카드별 연회비 본전 현황
	 */
	List<AnnualFeeBreakEvenResponseDto> getAnnualFeeBreakEven(
		Long userId,
		int year
	);
}
