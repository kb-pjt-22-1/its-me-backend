package site.benepay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import site.benepay.service.BookmarkService;
import site.benepay.service.CardPerformanceService;
import site.benepay.service.CatalogService;
import site.benepay.service.HomeService;
import site.benepay.service.OutboxDeliveryService;
import site.benepay.service.PaymentQueryService;
import site.benepay.service.PaymentService;
import site.benepay.service.PaymentTokenService;
import site.benepay.service.PinAuthService;
import site.benepay.service.RecommendationService;

@Configuration
public class RootConfig {

    @Bean
    public BookmarkService bookmarkService() {
        return new BookmarkService();
    }

    @Bean
    public CardPerformanceService cardPerformanceService() {
        return new CardPerformanceService();
    }

    @Bean
    public CatalogService catalogService() {
        return new CatalogService();
    }

    @Bean
    public HomeService homeService() {
        return new HomeService();
    }

    @Bean
    public OutboxDeliveryService outboxDeliveryService() {
        return new OutboxDeliveryService();
    }

    @Bean
    public PaymentQueryService paymentQueryService() {
        return new PaymentQueryService();
    }

    @Bean
    public PaymentService paymentService() {
        return new PaymentService();
    }

    @Bean
    public PaymentTokenService paymentTokenService() {
        return new PaymentTokenService();
    }

    @Bean
    public PinAuthService pinAuthService() {
        return new PinAuthService();
    }

    @Bean
    public RecommendationService recommendationService() {
        return new RecommendationService();
    }

    @Bean
    public PaymentApplicationLifecycle paymentApplicationLifecycle(OutboxDeliveryService outboxDeliveryService) {
        return new PaymentApplicationLifecycle(outboxDeliveryService);
    }
}
