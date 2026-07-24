package site.benepay.controller;

import site.benepay.service.PaymentQueryService;
import site.benepay.util.DemoUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/v1/payments")
public class PaymentHistoryServlet extends BaseApiServlet {
    private final PaymentQueryService paymentQueryService = new PaymentQueryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeSuccess(response, paymentQueryService.findAll(DemoUser.USER_ID));
        } catch (Exception e) {
            handleError(response, e);
        }
    }
}
