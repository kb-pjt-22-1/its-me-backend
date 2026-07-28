package site.benepay.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import site.benepay.config.AppConfig;
import site.benepay.config.Database;
import site.benepay.domain.Merchant;
import site.benepay.domain.PaymentTokenRecord;
import site.benepay.domain.PaymentView;
import site.benepay.domain.UserCard;
import site.benepay.repository.CardRepository;
import site.benepay.repository.MerchantRepository;
import site.benepay.repository.PaymentRepository;
import site.benepay.repository.PaymentTokenRepository;
import site.benepay.util.HashUtil;
import site.benepay.util.QrCodeUtil;
import site.benepay.util.TokenGenerator;

public class PaymentTokenService {
	private final CardRepository cards = new CardRepository();
	private final MerchantRepository merchants = new MerchantRepository();
	private final PaymentTokenRepository tokens = new PaymentTokenRepository();
	private final PaymentRepository payments = new PaymentRepository();
	private final int ttl = AppConfig.getInt("token.ttlSeconds", 60);

	public Map<String, Object> issue(long userId, long cardId, Long merchantId, BigDecimal amount) {
		boolean hasMerchant = merchantId != null && merchantId > 0, hasAmount = amount != null;
		if (hasMerchant != hasAmount)
			throw new ServiceException(400, "INVALID_PAYMENT_CONTEXT", "가맹점과 결제 금액을 함께 전달해야 합니다.");
		if (hasAmount)
			validateAmount(amount);
		try (Connection c = Database.getConnection()) {
			UserCard card = cards.findById(c, userId, cardId);
			if (card == null)
				throw new ServiceException(404, "CARD_NOT_FOUND", "결제 카드를 찾을 수 없습니다.");
			Merchant merchant = null;
			if (hasMerchant) {
				merchant = merchants.findById(c, merchantId);
				if (merchant == null)
					throw new ServiceException(404, "MERCHANT_NOT_FOUND", "가맹점을 찾을 수 없습니다.");
			}
			tokens.revokeActive(c, userId, cardId);
			String raw = TokenGenerator.generatePaymentToken(), hash = HashUtil.sha256Hex(raw);
			LocalDateTime expires = LocalDateTime.now().plusSeconds(ttl);
			long id = tokens.insert(c, userId, cardId, merchantId, amount, hash, expires);
			Map<String, Object> r = new LinkedHashMap<String, Object>();
			r.put("paymentTokenId", id);
			r.put("paymentToken", raw);
			r.put("tokenStatus", "ACTIVE");
			r.put("expiresAt", expires.toString());
			r.put("expiresInSeconds", ttl);
			r.put("merchantId", merchantId);
			r.put("merchantName", merchant == null ? null : merchant.getMerchantName());
			r.put("amount", amount);
			r.put("userCardId", cardId);
			r.put("cardName", card.getCardName());
			r.put("cardCompanyName", card.getCardCompanyName());
			r.put("cardLast4", card.getCardLast4());
			r.put("qrImage", QrCodeUtil.qrDataUrl(raw));
			r.put("barcodeImage", QrCodeUtil.barcodeDataUrl(raw));
			String numeric = hash.replaceAll("[^0-9]", "");
			while (numeric.length() < 14)
				numeric += id;
			r.put("displayCode", numeric.substring(0, 14));
			return r;
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(500, "TOKEN_ISSUE_FAILED", "결제 코드 발급에 실패했습니다.", e);
		}
	}

	public Map<String, Object> status(long userId, long id) {
		try (Connection c = Database.getConnection()) {
			PaymentTokenRecord t = tokens.findById(c, id, userId);
			if (t == null)
				throw new ServiceException(404, "TOKEN_NOT_FOUND", "결제 토큰을 찾을 수 없습니다.");
			String status = t.getTokenStatus();
			if ("ACTIVE".equals(status) && t.getExpiresAt().isBefore(LocalDateTime.now())) {
				tokens.markExpired(c, id);
				status = "EXPIRED";
			}
			PaymentView payment = payments.findByTokenId(c, id, userId);
			Map<String, Object> r = new LinkedHashMap<String, Object>();
			r.put("paymentTokenId", id);
			r.put("tokenStatus", status);
			r.put("expiresAt", t.getExpiresAt().toString());
			r.put("payment", payment);
			return r;
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(500, "TOKEN_STATUS_FAILED", "결제 상태 조회에 실패했습니다.", e);
		}
	}

	private void validateAmount(BigDecimal a) {
		if (a == null || a.compareTo(BigDecimal.ONE) < 0 || a.compareTo(new BigDecimal("10000000")) > 0)
			throw new ServiceException(400, "INVALID_AMOUNT", "결제 금액은 1원 이상 1,000만원 이하여야 합니다.");
	}
}
