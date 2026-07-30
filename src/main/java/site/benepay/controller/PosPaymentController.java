package site.benepay.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.domain.PaymentView;
import site.benepay.dto.ApiResponse;
import site.benepay.service.PaymentService;

@RestController
@RequestMapping("/api/v1/pos/payments")
public class PosPaymentController {
    private final PaymentService paymentService;

    public PosPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<PaymentView> approve(@RequestBody PosPaymentRequest body) {
        PaymentView result = paymentService.approve(
            body.getPaymentToken(),
            body.getMerchantId(),
            body.getAmount(),
            body.getPosRequestId(),
            body.isSimulateFailure()
        );
        return ApiResponse.success(result);
    }

    public static class PosPaymentRequest {
        private String paymentToken;
        private long merchantId;
        private BigDecimal amount;
        private String posRequestId;
        private boolean simulateFailure;

        public String getPaymentToken() {
            return paymentToken;
        }

        public void setPaymentToken(String paymentToken) {
            this.paymentToken = paymentToken;
        }

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

        public String getPosRequestId() {
            return posRequestId;
        }

        public void setPosRequestId(String posRequestId) {
            this.posRequestId = posRequestId;
        }

        public boolean isSimulateFailure() {
            return simulateFailure;
        }

        public void setSimulateFailure(boolean simulateFailure) {
            this.simulateFailure = simulateFailure;
        }
    }
}
