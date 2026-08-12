package site.benepay.domain.benefit.service;

import java.util.List;

import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto;

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

	/**
	 * 사용자가 이번 달(또는 지정한 달) 받은 혜택 총액과 카테고리별 내역을 조회한다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param yearMonth 조회 기준 연월(yyyyMM), null이면 이번 달
	 * @return 총액/증감/카테고리별 혜택 리포트
	 */
	MonthlyBenefitReportResponseDto getMonthlyBenefitReport(
		Long userId,
		String yearMonth
	);
}
