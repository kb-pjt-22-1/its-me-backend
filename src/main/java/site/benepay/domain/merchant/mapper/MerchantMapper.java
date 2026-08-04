package site.benepay.domain.merchant.mapper;

import org.apache.ibatis.annotations.Param;
import site.benepay.domain.merchant.vo.Merchant;
import site.benepay.domain.merchant.vo.RecommendationMerchantCandidate;

import java.util.List;
import java.util.Optional;


public interface MerchantMapper {

    void insert(Merchant merchant);

    Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

    List<Merchant> findAll();

    List<RecommendationMerchantCandidate> findNearbyRecommendationCandidates(
            @Param("userId") Long userId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusMeters") Integer radiusMeters,
            @Param("categoryCode") String categoryCode,
            @Param("candidateLimit") Integer candidateLimit
    );

    Optional<RecommendationMerchantCandidate> findRecommendationCandidateByMerchantId(
            @Param("userId") Long userId,
            @Param("merchantId") Long merchantId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );

    boolean existsByMerchantCode(@Param("merchantCode") String merchantCode);

    int update(Merchant merchant);

    int deleteByMerchantId(@Param("merchantId") Long merchantId);
}
