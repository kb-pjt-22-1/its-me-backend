package site.benepay.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import site.benepay.config.Database;
import site.benepay.domain.BenefitResult;
import site.benepay.domain.Merchant;
import site.benepay.domain.PaymentTokenRecord;
import site.benepay.domain.PaymentView;
import site.benepay.domain.UserCard;
import site.benepay.repository.CardRepository;
import site.benepay.repository.MerchantRepository;
import site.benepay.repository.OutboxRepository;
import site.benepay.repository.PaymentRepository;
import site.benepay.repository.PaymentTokenRepository;
import site.benepay.util.HashUtil;
import site.benepay.util.JsonUtil;
import site.benepay.util.TokenGenerator;

public class PaymentService {
	private final PaymentTokenRepository tokenRepository = new PaymentTokenRepository();
	private final PaymentRepository paymentRepository = new PaymentRepository();
	private final CardRepository cardRepository = new CardRepository();
	private final MerchantRepository merchantRepository = new MerchantRepository();
	private final BenefitCalculationService benefitService = new BenefitCalculationService();
	private final OutboxRepository outboxRepository = new OutboxRepository();
	private final OutboxDeliveryService deliveryService = new OutboxDeliveryService();

	public PaymentView approve(String paymentToken, long merchantId, BigDecimal amount, String posRequestId,
		boolean simulateFailure) {
		validateRequest(paymentToken, merchantId, amount, posRequestId);
		long paymentId;
		String status;
		try (Connection connection = Database.getConnection()) {
			connection.setAutoCommit(false);
			try {
				PaymentView duplicate = paymentRepository.findByPosRequestId(connection, posRequestId);
				if (duplicate != null) {
					connection.commit();
					return duplicate;
				}
				PaymentTokenRecord token = tokenRepository.findByHashForUpdate(connection,
					HashUtil.sha256Hex(paymentToken));
				if (token == null)
					throw new ServiceException(404, "TOKEN_NOT_FOUND", "유효한 결제 토큰을 찾을 수 없습니다.");
				if (!"ACTIVE".equals(token.getTokenStatus()))
					throw new ServiceException(409, "TOKEN_UNAVAILABLE", "이미 사용·폐기되었거나 사용할 수 없는 결제 토큰입니다.");
				if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
					tokenRepository.markExpired(connection, token.getPaymentTokenId());
					connection.commit();
					throw new ServiceException(410, "TOKEN_EXPIRED", "결제 토큰의 유효시간이 만료되었습니다.");
				}
				if (token.getMerchantId() != null && token.getMerchantId().longValue() != merchantId)
					throw new ServiceException(409, "MERCHANT_MISMATCH", "결제 코드에 지정된 매장과 POS 매장이 다릅니다.");
				if (token.getExpectedAmount() != null && token.getExpectedAmount().compareTo(amount) != 0)
					throw new ServiceException(409, "AMOUNT_MISMATCH", "결제 코드에 지정된 금액과 POS 결제 금액이 다릅니다.");
				UserCard card = cardRepository.findById(connection, token.getUserId(), token.getUserCardId());
				if (card == null)
					throw new ServiceException(409, "CARD_UNAVAILABLE", "카드가 정지·삭제되었거나 사용할 수 없습니다.");
				Merchant merchant = merchantRepository.findById(connection, merchantId);
				if (merchant == null)
					throw new ServiceException(404, "MERCHANT_NOT_FOUND", "가맹점을 찾을 수 없습니다.");
				BenefitResult benefit = benefitService.calculate(connection, card, merchant, amount);
				boolean limitFailure = amount.compareTo(new BigDecimal("1000000")) > 0;
				status = (simulateFailure || limitFailure) ? "DECLINED" : "APPROVED";
				String failureReason = null, approvalCode = null;
				if ("DECLINED".equals(status)) {
					benefit = benefitService.noBenefit(amount);
					failureReason = simulateFailure ? "가상 POS 승인 실패 시뮬레이션" : "1회 결제 한도 1,000,000원 초과";
				} else
					approvalCode = TokenGenerator.generateApprovalCode();
				paymentId = paymentRepository.insert(connection, token.getUserCardId(), merchantId,
					token.getPaymentTokenId(), posRequestId, amount, benefit, status, approvalCode, failureReason);
				if ("APPROVED".equals(status))
					outboxRepository.insert(connection, paymentId, "APPROVED",
						payload(paymentId, token.getUserCardId(), amount, "APPROVED"));
				tokenRepository.markUsed(connection, token.getPaymentTokenId());
				connection.commit();
			} catch (Exception e) {
				try {
					connection.rollback();
				} catch (Exception ignored) {
				}
				throw e;
			}
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(500, "PAYMENT_APPROVAL_FAILED", "결제 승인 처리 중 오류가 발생했습니다.", e);
		}
		if ("APPROVED".equals(status))
			deliveryService.deliver(paymentId, "APPROVED");
		try (Connection c = Database.getConnection()) {
			PaymentView result = paymentRepository.findById(c, paymentId);
			if (result == null)
				throw new ServiceException(500, "PAYMENT_READ_FAILED", "저장된 결제 결과를 읽을 수 없습니다.");
			return result;
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(500, "PAYMENT_READ_FAILED", "저장된 결제 결과를 읽을 수 없습니다.", e);
		}
	}

	private String payload(long paymentId, long userCardId, BigDecimal amount, String type) {
		Map<String, Object> x = new LinkedHashMap<String, Object>();
		x.put("paymentId", paymentId);
		x.put("userCardId", userCardId);
		x.put("amount", amount);
		x.put("eventType", type);
		x.put("occurredAt", LocalDateTime.now().toString());
		return JsonUtil.gson().toJson(x);
	}

	private void validateRequest(String token, long merchantId, BigDecimal amount, String posRequestId) {
		if (token == null || token.trim().isEmpty())
			throw new ServiceException(400, "TOKEN_REQUIRED", "결제 토큰이 필요합니다.");
		if (merchantId <= 0)
			throw new ServiceException(400, "MERCHANT_REQUIRED", "가맹점 정보가 필요합니다.");
		if (amount == null || amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("10000000")) > 0)
			throw new ServiceException(400, "INVALID_AMOUNT", "결제 금액은 1원 이상 1,000만원 이하여야 합니다.");
		if (posRequestId == null || posRequestId.trim().isEmpty() || posRequestId.length() > 64)
			throw new ServiceException(400, "INVALID_POS_REQUEST_ID", "올바른 POS 요청 식별자가 필요합니다.");
	}
}
