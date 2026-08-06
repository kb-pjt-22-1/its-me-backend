package site.benepay.domain.card.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.card.vo.UserCardBenefitVO;
import site.benepay.domain.card.vo.UserCardDetailVO;
import site.benepay.domain.card.vo.UserCardListVO;
import site.benepay.domain.card.vo.UserCardPerformanceVO;

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

	int updateRecommendationEnabled(@Param("userId") Long userId, @Param("userCardId") Long userCardId,
		@Param("recommendationEnabled") Boolean recommendationEnabled);
}
