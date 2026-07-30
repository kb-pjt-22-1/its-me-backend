package site.benepay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.HomeService;
import site.benepay.util.DemoUser;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public ApiResponse<Object> home() {
        return ApiResponse.success(homeService.load(DemoUser.USER_ID));
    }
}
