package site.benepay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.PaymentQueryService;
import site.benepay.util.DemoUser;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentHistoryController {
    private final PaymentQueryService paymentQueryService;

    public PaymentHistoryController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping
    public ApiResponse<Object> findMonthly(
        @RequestParam(required = false) String yearMonth
    ) {
        return ApiResponse.success(paymentQueryService.findMonthly(DemoUser.USER_ID, yearMonth));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<Object> findDetail(@PathVariable long paymentId) {
        return ApiResponse.success(paymentQueryService.findDetail(DemoUser.USER_ID, paymentId));
    }
}
