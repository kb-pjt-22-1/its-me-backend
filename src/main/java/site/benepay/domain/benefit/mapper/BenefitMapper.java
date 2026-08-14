package site.benepay.domain.benefit.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.benefit.dto.DailyBenefitAmountDto;
import site.benepay.domain.benefit.vo.CategoryBenefitUsageVO;
import site.benepay.domain.benefit.vo.HeldCardBenefitVO;
import site.benepay.domain.benefit.vo.MonthlyCategoryBenefitVO;

public interface BenefitMapper {

	/**
	 * 사용자가 보유한 연회비 카드들의 일별 혜택 금액을 조회한다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param startPaymentTime 조회 시작 시각
	 * @param endPaymentTime 조회 종료 시각, 해당 시각은 포함하지 않음
	 * @return 카드별 일별 혜택 목록
	 */
	List<DailyBenefitAmountDto> findAnnualFeeBenefitsByUserId(
		@Param("userId") Long userId,
		@Param("startPaymentTime") LocalDateTime startPaymentTime,
		@Param("endPaymentTime") LocalDateTime endPaymentTime
	);

	/**
	 * 사용자가 보유한 전체 카드의 결제 내역을, 지정한 기간 동안 가맹점 카테고리별로
	 * 묶어 혜택(할인) 합계를 조회한다. 월간 리포트(#47/#50) 전용.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param startPaymentTime 조회 시작 시각
	 * @param endPaymentTime 조회 종료 시각, 해당 시각은 포함하지 않음
	 * @return 카테고리별 혜택 합계 목록, 금액 내림차순
	 */
	List<MonthlyCategoryBenefitVO> findMonthlyCategoryBenefitsByUserId(
		@Param("userId") Long userId,
		@Param("startPaymentTime") LocalDateTime startPaymentTime,
		@Param("endPaymentTime") LocalDateTime endPaymentTime
	);

	/**
	 * 카테고리별 혜택 현황(#카드 전체 한도 소진 현황) 전용. 사용자가 보유한 활성 카드 전체의
	 * benefits_info + 전월 실적(활성 구간 판정용)을 조회한다. 추천 도메인의
	 * findRecommendationCardsByUserId와 달리 recommendation_enabled 필터가 없다 - "보유한
	 * 전체 카드"를 봐야 하는 기능이라 추천 노출 여부와 무관해야 한다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param previousYearMonth 전월(yyyyMM) - activeTier 판정 기준
	 * @return 보유 카드별 benefits_info + 전월 실적
	 */
	List<HeldCardBenefitVO> findHeldCardBenefitsByUserId(
		@Param("userId") Long userId,
		@Param("previousYearMonth") String previousYearMonth
	);

	/**
	 * 카드+카테고리 단위로 이번 달 실제 소진한 혜택 금액/횟수를 조회한다(카테고리별 혜택 현황
	 * 전용). discount_amount &gt; 0인 결제만 "혜택이 적용된 결제"로 센다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param startPaymentTime 조회 시작 시각
	 * @param endPaymentTime 조회 종료 시각, 해당 시각은 포함하지 않음
	 * @return 카드+카테고리별 소진 금액/횟수
	 */
	List<CategoryBenefitUsageVO> findCategoryBenefitUsageByUserId(
		@Param("userId") Long userId,
		@Param("startPaymentTime") LocalDateTime startPaymentTime,
		@Param("endPaymentTime") LocalDateTime endPaymentTime
	);
}
