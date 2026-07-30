package site.benepay.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.config.AppConfig;
import site.benepay.dto.ApiResponse;
import site.benepay.service.CardPerformanceService;
import site.benepay.service.ServiceException;

@RestController
@RequestMapping("/internal/api/v1/card-performance-events")
public class CardPerformanceInternalController {
    private final CardPerformanceService cardPerformanceService;

    public CardPerformanceInternalController(CardPerformanceService cardPerformanceService) {
        this.cardPerformanceService = cardPerformanceService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> apply(
        @RequestHeader(value = "X-Internal-Key", required = false) String internalKey,
        @RequestBody EventRequest body
    ) {
        String expected = AppConfig.get("internal.api.key", "benepay-demo-key");
        if (!expected.equals(internalKey)) {
            throw new ServiceException(401, "INVALID_INTERNAL_KEY", "내부 API 인증에 실패했습니다.");
        }

        boolean applied = cardPerformanceService.apply(body.getPaymentId(), body.getEventType());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applied", applied);
        result.put("paymentId", body.getPaymentId());
        result.put("eventType", body.getEventType());
        return ApiResponse.success(result);
    }

    public static class EventRequest {
        private long paymentId;
        private String eventType;

        public long getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(long paymentId) {
            this.paymentId = paymentId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }
    }
}
