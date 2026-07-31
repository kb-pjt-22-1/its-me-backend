package site.benepay.domain.recommendation.mapper;

import org.apache.ibatis.annotations.Param;
import site.benepay.domain.recommendation.model.BenefitStoreCandidate;
import site.benepay.domain.recommendation.model.StoreBenefitCardCandidate;

import java.util.List;

public interface RecommendationMapper {

    List<BenefitStoreCandidate> getNearbyBenefitStoreCandidates(
            @Param("userId") Long userId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusMeters") Integer radiusMeters,
            @Param("categoryCode") String categoryCode,
            @Param("candidateLimit") Integer candidateLimit
    );

    List<StoreBenefitCardCandidate> getBenefitStoreCardCandidates(
            @Param("userId") Long userId,
            @Param("merchantId") Long merchantId,
<<<<<<< Updated upstream
=======
            @Param("performanceMonth") String performanceMonth,
>>>>>>> Stashed changes
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );
}
