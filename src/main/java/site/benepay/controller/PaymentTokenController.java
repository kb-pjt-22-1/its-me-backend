package site.benepay.controller;

import java.math.BigDecimal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.PaymentTokenService;
import site.benepay.util.DemoUser;
import site.benepay.util.SessionAuthUtil;

@RestController
@RequestMapping("/api/v1/payment-tokens")
public class PaymentTokenController {
    private final PaymentTokenService paymentTokenService;

    public PaymentTokenController(PaymentTokenService paymentTokenService) {
        this.paymentTokenService = paymentTokenService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> issue(
        @RequestBody TokenIssueRequest body,
        HttpServletRequest request
    ) {
        SessionAuthUtil.requirePinVerified(request);
        Map<String, Object> result = paymentTokenService.issue(
            DemoUser.USER_ID,
            body.getUserCardId(),
            body.getMerchantId(),
            body.getAmount()
        );
        request.getSession(true).setAttribute(SessionAuthUtil.LATEST_PAYMENT_TOKEN, result);
        return ApiResponse.success(result);
    }

    @GetMapping("/{paymentTokenId}")
    public ApiResponse<Object> status(@PathVariable long paymentTokenId) {
        return ApiResponse.success(paymentTokenService.status(DemoUser.USER_ID, paymentTokenId));
    }

    public static class TokenIssueRequest {
        private long userCardId;
        private Long merchantId;
        private BigDecimal amount;

        public long getUserCardId() {
            return userCardId;
        }

        public void setUserCardId(long userCardId) {
            this.userCardId = userCardId;
        }

        public Long getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(Long merchantId) {
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
