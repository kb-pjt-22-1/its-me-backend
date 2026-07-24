package site.benepay.controller;

import site.benepay.service.PinAuthService;
import site.benepay.util.DemoUser;
import site.benepay.util.SessionAuthUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/v1/auth/pin/verify")
public class PinVerifyServlet extends BaseApiServlet {
    private final PinAuthService pinAuthService = new PinAuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PinRequest body = readJson(request, PinRequest.class);
            Map<String, Object> result = pinAuthService.verify(DemoUser.USER_ID, body.pin);
            SessionAuthUtil.markPinVerified(request);
            writeSuccess(response, result);
        } catch (Exception e) {
            handleError(response, e);
        }
    }

    private static class PinRequest {
        String pin;
    }
}
