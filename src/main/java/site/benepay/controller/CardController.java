package site.benepay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.CatalogService;
import site.benepay.util.DemoUser;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {
    private final CatalogService catalogService;

    public CardController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiResponse<Object> cards() {
        return ApiResponse.success(catalogService.cards(DemoUser.USER_ID));
    }
}
