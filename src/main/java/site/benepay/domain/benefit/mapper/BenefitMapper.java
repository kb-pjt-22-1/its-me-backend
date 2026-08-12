package site.benepay.domain.benefit.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.benefit.vo.DailyBenefitAmountVO;

public interface BenefitMapper {

	/**
	 * 사용자가 보유한 연회비 카드들의 일별 혜택 금액을 조회한다.
	 *
	 * @param userId 로그인 사용자 ID
	 * @param startPaymentTime 조회 시작 시각
	 * @param endPaymentTime 조회 종료 시각, 해당 시각은 포함하지 않음
	 * @return 카드별 일별 혜택 목록
	 */
	List<DailyBenefitAmountVO> findAnnualFeeBenefitsByUserId(
		@Param("userId") Long userId,
		@Param("startPaymentTime") LocalDateTime startPaymentTime,
		@Param("endPaymentTime") LocalDateTime endPaymentTime
	);
}
