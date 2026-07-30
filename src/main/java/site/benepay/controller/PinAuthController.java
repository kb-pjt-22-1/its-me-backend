package site.benepay.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.PinAuthService;
import site.benepay.util.DemoUser;
import site.benepay.util.SessionAuthUtil;

@RestController
@RequestMapping("/api/v1/auth/pin")
public class PinAuthController {
    private final PinAuthService pinAuthService;

    public PinAuthController(PinAuthService pinAuthService) {
        this.pinAuthService = pinAuthService;
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(
        @RequestBody PinRequest body,
        HttpServletRequest request
    ) {
        Map<String, Object> result = pinAuthService.verify(DemoUser.USER_ID, body.getPin());
        SessionAuthUtil.markPinVerified(request);
        return ApiResponse.success(result);
    }

    public static class PinRequest {
        private String pin;

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }
    }
}
