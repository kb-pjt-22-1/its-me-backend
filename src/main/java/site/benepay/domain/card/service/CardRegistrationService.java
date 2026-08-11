package site.benepay.domain.card.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.card.vo.UserCardVO;
import site.benepay.integration.kbcard.dto.KbCardResponseDto;

/**
 * 외부 카드 정보를 BenePay의 user_cards로 등록하는 공통 서비스다.
 *
 * 최초 회원가입 동기화와 CARD_ISSUED Webhook 처리가
 * 모두 이 서비스를 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardRegistrationService {

	private final CardMapper cardMapper;

	/**
	 * 여러 카드 중 등록 가능한 신규 카드만 INSERT하고 등록 건수를 반환한다.
	 */
	@Transactional
	public int registerCards(Long userId, List<KbCardResponseDto> cards) {
		if (cards == null || cards.isEmpty()) {
			return 0;
		}

		boolean primaryCardExists =
			cardMapper.existsPrimaryCardByUserId(userId);

		int insertedCount = 0;

		for (KbCardResponseDto kbCard : cards) {
			/*
			 * 현재 user_cards는 토큰이 발급된 카드만 저장하는 구조다.
			 * 개발 단계에서는 ACTIVE 카드와 ACTIVE 토큰만 자동 등록한다.
			 */
			if (!isLinkable(kbCard)) {
				log.info(
					"연동 대상이 아닌 카드 건너뜀. cardReferenceId={}",
					kbCard.getCardReferenceId()
				);
				continue;
			}

			Long cardId = cardMapper
				.findCardIdByIssuerProductCode(kbCard.getProductCode())
				.orElse(null);

			if (cardId == null) {
				/*
				 * Mock 상품 코드와 BenePay cards가 맞지 않는 설정 오류다.
				 * 다른 정상 카드까지 막지 않도록 최초 일괄 동기화에서는 건너뛴다.
				 */
				log.warn(
					"BenePay에 없는 KB 상품 코드. productCode={}",
					kbCard.getProductCode()
				);
				continue;
			}

			UserCardVO userCard = UserCardVO.builder()
				.userId(userId)
				.cardId(cardId)
				.issuerCardReferenceId(kbCard.getCardReferenceId())
				.issuerTokenReferenceId(kbCard.getTokenReferenceId())
				.paymentToken(kbCard.getPaymentToken())
				.cardExpiryYearMonth(kbCard.getCardExpiryYearMonth())
				.tokenExpiryDate(kbCard.getTokenExpiryDate())
				.panLast4(kbCard.getPanLast4())
				.status(kbCard.getCardStatus())
				/*
				 * 사용자의 첫 번째 카드만 대표 카드로 설정한다.
				 */
				.primaryCard(!primaryCardExists)
				.recommendationEnabled(true)
				.build();

			/*
			 * INSERT SQL 자체에도 NOT EXISTS를 사용한다.
			 * 사전 조회만 사용하는 것보다 동시 요청에 안전하다.
			 */
			int inserted = cardMapper.insertUserCardIfAbsent(userCard);

			if (inserted == 1) {
				insertedCount++;
				primaryCardExists = true;
			}
		}

		return insertedCount;
	}

	private boolean isLinkable(KbCardResponseDto card) {
		return "ACTIVE".equals(card.getCardStatus())
			&& "ACTIVE".equals(card.getTokenStatus())
			&& card.getTokenReferenceId() != null
			&& card.getPaymentToken() != null
			&& card.getTokenExpiryDate() != null;
	}
}
