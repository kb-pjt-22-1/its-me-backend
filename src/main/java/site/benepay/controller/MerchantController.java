package site.benepay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.CatalogService;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {
    private final CatalogService catalogService;

    public MerchantController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiResponse<Object> merchants() {
        return ApiResponse.success(catalogService.merchants());
    }
}
