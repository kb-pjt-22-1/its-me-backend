package site.benepay.domain.benefit.service;

import java.util.List;

import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.BenefitCoachResponseDto;
import site.benepay.domain.benefit.dto.CategoryBenefitStatusResponseDto;
import site.benepay.domain.benefit.dto.ExpiringBenefitsResponseDto;
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

	/**
	 * 최근 3개월 결제 패턴과 보유 카드 혜택을 분석하여
	 * AI 혜택 코칭 결과를 생성한다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @return AI 혜택 코칭 결과
	 */
	BenefitCoachResponseDto getBenefitCoaching(Long userId);

	/**
	 * 사용자가 보유한 전체 카드의 카테고리별 혜택 현황(금액 한도/횟수 한도 대비 소진량)을
	 * 조회한다. 이번 달 적용 중인 혜택은 전월 실적으로 정해지는 구간(activeTier) 기준이고,
	 * 소진량은 이번 달 실제 결제 기준이다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param yearMonth 조회 기준 연월(yyyyMM), null이면 이번 달
	 * @return 카드x카테고리x혜택 단위의 한도 소진 현황 목록
	 */
	List<CategoryBenefitStatusResponseDto> getCategoryBenefitStatus(
		Long userId,
		String yearMonth
	);

	/**
	 * 이번 달 적용 중인 혜택 중 아직 안 쓴 것을 금액 큰 순으로 최대 3개 조회한다("놓치기 쉬운
	 * 혜택", #48). 카테고리 한정 혜택뿐 아니라 브랜드 한정/ALL_MERCHANTS 혜택도 대상이다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @return 미사용 혜택 상위 3개 + 이번 달 남은 일수
	 */
	ExpiringBenefitsResponseDto getExpiringBenefits(Long userId);
}