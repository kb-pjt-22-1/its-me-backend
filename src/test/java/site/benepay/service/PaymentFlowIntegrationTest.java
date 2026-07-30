package site.benepay.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import site.benepay.config.DatabaseInitializer;
import site.benepay.domain.PaymentView;

class PaymentFlowIntegrationTest {

    @BeforeAll
    static void initializeDatabase() {
        DatabaseInitializer.initialize();
    }

    @Test
    void pinTokenApprovalAndHistoryFlowWorks() {
        Map<String, Object> pinResult = new PinAuthService().verify(1L, "123456");
        assertEquals(Boolean.TRUE, pinResult.get("verified"));

        PaymentTokenService tokenService = new PaymentTokenService();
        Map<String, Object> tokenResult = tokenService.issue(
            1L,
            1L,
            3L,
            new BigDecimal("4800")
        );

        String paymentToken = String.valueOf(tokenResult.get("paymentToken"));
        assertFalse(paymentToken.isBlank());
        assertTrue(String.valueOf(tokenResult.get("qrImage")).startsWith("data:image/png;base64,"));
        assertTrue(String.valueOf(tokenResult.get("barcodeImage")).startsWith("data:image/png;base64,"));

        PaymentView approved = new PaymentService().approve(
            paymentToken,
            3L,
            new BigDecimal("4800"),
            "JUNIT-" + System.nanoTime(),
            false
        );

        assertEquals("APPROVED", approved.getPaymentStatus());

        PaymentQueryService queryService = new PaymentQueryService();
        PaymentView detail = queryService.findDetail(1L, approved.getPaymentId());
        assertNotNull(detail);
        assertEquals(approved.getPaymentId(), detail.getPaymentId());

        Object monthlyResult = queryService.findMonthly(1L, null);
        assertNotNull(monthlyResult);
    }

    @Test
    void cardMerchantRecommendationAndBookmarkFunctionsRemainAvailable() {
        CatalogService catalogService = new CatalogService();
        assertFalse(((List<?>) catalogService.cards(1L)).isEmpty());
        assertFalse(((List<?>) catalogService.merchants()).isEmpty());

        Object recommendation = new RecommendationService().recommend(
            1L,
            3L,
            new BigDecimal("4800")
        );
        assertNotNull(recommendation);

        BookmarkService bookmarkService = new BookmarkService();
        bookmarkService.save(1L, 3L);
        assertFalse(((List<?>) bookmarkService.findAll(1L)).isEmpty());
        bookmarkService.delete(1L, 3L);
    }
}
