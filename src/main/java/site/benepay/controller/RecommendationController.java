package site.benepay.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.RecommendationService;
import site.benepay.util.DemoUser;

@RestController
@RequestMapping({"/api/v1/recommend", "/api/v1/payment-recommendations"})
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ApiResponse<Object> recommend(
        @RequestParam long merchantId,
        @RequestParam BigDecimal amount
    ) {
        return ApiResponse.success(
            recommendationService.recommend(DemoUser.USER_ID, merchantId, amount)
        );
    }

    @PostMapping
    public ApiResponse<Object> recommend(@RequestBody RecommendationRequest body) {
        return ApiResponse.success(
            recommendationService.recommend(DemoUser.USER_ID, body.getMerchantId(), body.getAmount())
        );
    }

    public static class RecommendationRequest {
        private long merchantId;
        private BigDecimal amount;

        public long getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(long merchantId) {
            this.merchantId = merchantId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
