package site.benepay.domain.card.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.card.vo.CardMonthlyStatusVO;
import site.benepay.domain.card.vo.UserCardBenefitVO;
import site.benepay.domain.card.vo.UserCardDetailVO;
import site.benepay.domain.card.vo.UserCardListVO;
import site.benepay.domain.card.vo.UserCardPerformanceVO;
import site.benepay.domain.card.vo.UserCardRecommendationVO;
import site.benepay.domain.card.vo.UserCardVO;

public interface CardMapper {

	List<UserCardListVO> findAllByUserId(@Param("userId") Long userId);

	Optional<UserCardDetailVO> findDetailByUserCardId(@Param("userId") Long userId,
		@Param("userCardId") Long userCardId);

	Optional<UserCardBenefitVO> findBenefitsByUserCardId(@Param("userId") Long userId,
		@Param("userCardId") Long userCardId);

	Optional<UserCardPerformanceVO> findPerformanceByUserCardId(@Param("userId") Long userId,
		@Param("userCardId") Long userCardId, @Param("yearMonth") String yearMonth
	);

	boolean existsActiveUserCard(@Param("userId") Long userId, @Param("userCardId") Long userCardId);

	int clearPrimaryCard(@Param("userId") Long userId);

	int setPrimaryCard(@Param("userId") Long userId, @Param("userCardId") Long userCardId);

	// 카드 추천 여부 갱신
	int updateRecommendationEnabled(@Param("userId") Long userId, @Param("userCardId") Long userCardId,
		@Param("recommendationEnabled") Boolean recommendationEnabled);

	// 카드 실적 갱신
	int addMonthlySpending(@Param("userCardId") Long userCardId, @Param("targetYearMonth") String targetYearMonth,
		@Param("amount") BigDecimal amount
	);

	Optional<Long> findCardIdByIssuerProductCode(
		@Param("issuerProductCode") String issuerProductCode
	);

	boolean existsPrimaryCardByUserId(
		@Param("userId") Long userId
	);

	int insertUserCardIfAbsent(UserCardVO userCard);

	/**
	 * 사용자가 보유한 카드 중 추천에 사용할 수 있는 카드를 조회한다.
	 * ACTIVE 상태이며 추천 기능이 활성화된 카드만 조회한다.
	 */
	List<UserCardRecommendationVO> findRecommendationCardsByUserId(
		@Param("userId") Long userId
	);

	/**
	 * 사용자의 추천 대상 카드에 대한 월별 실적 이력을 조회한다.
	 *
	 * 완료된 과거 월 실적과 현재 월 누적 실적을
	 * 추천 서비스에 전달하기 위한 조회이다.
	 */
	List<CardMonthlyStatusVO> findMonthlyStatusByUserId(
		@Param("userId") Long userId
	);
}
