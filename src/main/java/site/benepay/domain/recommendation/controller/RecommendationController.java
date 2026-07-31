package site.benepay.domain.recommendation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.benepay.domain.recommendation.dto.request.BenefitStoreDetailRequestDto;
import site.benepay.domain.recommendation.dto.request.NearbyBenefitStoreRequestDto;
import site.benepay.domain.recommendation.dto.response.BenefitStoreDetailResponseDto;
import site.benepay.domain.recommendation.dto.response.NearbyBenefitStoreResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/nearby-benefit-stores")
    public ResponseEntity<NearbyBenefitStoreResponseDto> getNearbyBenefitStores(
            @ModelAttribute NearbyBenefitStoreRequestDto request) {
        return ResponseEntity.ok(recommendationService.getNearbyBenefitStores(currentUserId(), request));
    }

    @GetMapping("/benefit-stores/{merchantId}")
    public ResponseEntity<BenefitStoreDetailResponseDto> getBenefitStoreDetail(
            @PathVariable Long merchantId,
            @ModelAttribute BenefitStoreDetailRequestDto request
    ) {
        return ResponseEntity.ok(recommendationService.getBenefitStoreDetail(currentUserId(), merchantId, request));
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
