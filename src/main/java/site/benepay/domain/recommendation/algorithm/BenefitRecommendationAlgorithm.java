package site.benepay.domain.recommendation.algorithm;

import site.benepay.domain.recommendation.model.BenefitStoreCandidate;
import site.benepay.domain.recommendation.model.RecommendedBenefitCard;
import site.benepay.domain.recommendation.model.RecommendedBenefitStore;
import site.benepay.domain.recommendation.model.StoreBenefitCardCandidate;

<<<<<<< Updated upstream
=======
import java.math.BigDecimal;
>>>>>>> Stashed changes
import java.util.List;

public interface BenefitRecommendationAlgorithm {

    List<RecommendedBenefitStore> getRecommendedStores(List<BenefitStoreCandidate> candidates);

    List<RecommendedBenefitCard> getRecommendedCards(List<StoreBenefitCardCandidate> candidates);
<<<<<<< Updated upstream
=======

    List<RecommendedBenefitCard> getRecommendedCards(
            List<StoreBenefitCardCandidate> candidates,
            BigDecimal estimatedPaymentAmount
    );
>>>>>>> Stashed changes
}
