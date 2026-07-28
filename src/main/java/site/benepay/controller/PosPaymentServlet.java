package site.benepay.controller;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import site.benepay.domain.PaymentView;
import site.benepay.service.PaymentService;

@WebServlet("/api/v1/pos/payments")
public class PosPaymentServlet extends BaseApiServlet {
	private final PaymentService paymentService = new PaymentService();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			PosPaymentRequest body = readJson(request, PosPaymentRequest.class);
			PaymentView result = paymentService.approve(
				body.paymentToken,
				body.merchantId,
				body.amount,
				body.posRequestId,
				body.simulateFailure
			);
			writeSuccess(response, result);
		} catch (Exception e) {
			handleError(response, e);
		}
	}

	private static class PosPaymentRequest {
		String paymentToken;
		long merchantId;
		BigDecimal amount;
		String posRequestId;
		boolean simulateFailure;
	}
}
