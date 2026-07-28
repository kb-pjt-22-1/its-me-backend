package site.benepay.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import site.benepay.service.PaymentTokenService;
import site.benepay.service.ServiceException;
import site.benepay.util.DemoUser;
import site.benepay.util.SessionAuthUtil;

@WebServlet("/api/v1/payment-tokens/*")
public class PaymentTokenServlet extends BaseApiServlet {
	private final PaymentTokenService paymentTokenService = new PaymentTokenService();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			SessionAuthUtil.requirePinVerified(request);
			TokenIssueRequest body = readJson(request, TokenIssueRequest.class);
			Map<String, Object> result = paymentTokenService.issue(
				DemoUser.USER_ID, body.userCardId, body.merchantId, body.amount);
			request.getSession(true).setAttribute(SessionAuthUtil.LATEST_PAYMENT_TOKEN, result);
			writeSuccess(response, result);
		} catch (Exception e) {
			handleError(response, e);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String path = request.getPathInfo();
			if (path == null || path.length() <= 1) {
				throw new ServiceException(400, "TOKEN_ID_REQUIRED", "결제 토큰 ID가 필요합니다.");
			}
			String[] parts = path.substring(1).split("/");
			long tokenId = parseLong(parts[0], "paymentTokenId");
			writeSuccess(response, paymentTokenService.status(DemoUser.USER_ID, tokenId));
		} catch (Exception e) {
			handleError(response, e);
		}
	}

	private static class TokenIssueRequest {
		long userCardId;
		Long merchantId;
		BigDecimal amount;
	}
}
