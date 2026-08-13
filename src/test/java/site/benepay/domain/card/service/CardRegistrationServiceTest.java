package site.benepay.domain.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.card.vo.UserCardVO;
import site.benepay.integration.kbcard.dto.KbCardResponseDto;

@ExtendWith(MockitoExtension.class)
class CardRegistrationServiceTest {

	private static final Long USER_ID = 10L;
	private static final Long CARD_ID = 100L;

	@Mock
	private CardMapper cardMapper;

	@InjectMocks
	private CardRegistrationService cardRegistrationService;

	@Test
	@DisplayName("정상 카드 한 장을 모든 필드와 기본값에 맞게 등록한다")
	void registerCardsMapsAndRegistersOneCard() {
		KbCardResponseDto card = activeCard("card-ref-1", "product-1", "1111");
		when(cardMapper.existsPrimaryCardByUserId(USER_ID)).thenReturn(false);
		when(cardMapper.findCardIdByIssuerProductCode("product-1")).thenReturn(Optional.of(CARD_ID));
		when(cardMapper.insertUserCardIfAbsent(any(UserCardVO.class))).thenReturn(1);

		int result = cardRegistrationService.registerCards(USER_ID, List.of(card));

		assertThat(result).isEqualTo(1);
		ArgumentCaptor<UserCardVO> captor = ArgumentCaptor.forClass(UserCardVO.class);
		verify(cardMapper).insertUserCardIfAbsent(captor.capture());
		UserCardVO mapped = captor.getValue();
		assertThat(mapped.getUserId()).isEqualTo(USER_ID);
		assertThat(mapped.getCardId()).isEqualTo(CARD_ID);
		assertThat(mapped.getIssuerCardReferenceId()).isEqualTo("card-ref-1");
		assertThat(mapped.getIssuerTokenReferenceId()).isEqualTo("token-ref-card-ref-1");
		assertThat(mapped.getPaymentToken()).isEqualTo("payment-token-card-ref-1");
		assertThat(mapped.getCardExpiryYearMonth()).isEqualTo("202912");
		assertThat(mapped.getTokenExpiryDate()).isEqualTo(LocalDate.of(2029, 12, 31));
		assertThat(mapped.getPanLast4()).isEqualTo("1111");
		assertThat(mapped.getStatus()).isEqualTo("ACTIVE");
		assertThat(mapped.getPrimaryCard()).isTrue();
		assertThat(mapped.getRecommendationEnabled()).isTrue();
	}

	@Test
	@DisplayName("여러 정상 카드를 모두 등록하고 첫 번째 카드만 대표카드로 설정한다")
	void registerCardsRegistersMultipleCardsAndOnlyFirstIsPrimary() {
		KbCardResponseDto first = activeCard("card-ref-1", "product-1", "1111");
		KbCardResponseDto second = activeCard("card-ref-2", "product-2", "2222");
		when(cardMapper.existsPrimaryCardByUserId(USER_ID)).thenReturn(false);
		when(cardMapper.findCardIdByIssuerProductCode("product-1")).thenReturn(Optional.of(101L));
		when(cardMapper.findCardIdByIssuerProductCode("product-2")).thenReturn(Optional.of(102L));
		when(cardMapper.insertUserCardIfAbsent(any(UserCardVO.class))).thenReturn(1);

		int result = cardRegistrationService.registerCards(USER_ID, List.of(first, second));

		assertThat(result).isEqualTo(2);
		ArgumentCaptor<UserCardVO> captor = ArgumentCaptor.forClass(UserCardVO.class);
		verify(cardMapper, times(2)).insertUserCardIfAbsent(captor.capture());
		assertThat(captor.getAllValues())
			.extracting(UserCardVO::getCardId)
			.containsExactly(101L, 102L);
		assertThat(captor.getAllValues())
			.extracting(UserCardVO::getPrimaryCard)
			.containsExactly(true, false);
	}

	@Test
	@DisplayName("이미 등록된 카드 참조 ID는 INSERT 결과 0으로 중복 등록하지 않는다")
	void registerCardsDoesNotCountDuplicateCardReferenceId() {
		KbCardResponseDto card = activeCard("duplicate-ref", "product-1", "1111");
		when(cardMapper.existsPrimaryCardByUserId(USER_ID)).thenReturn(true);
		when(cardMapper.findCardIdByIssuerProductCode("product-1")).thenReturn(Optional.of(CARD_ID));
		when(cardMapper.insertUserCardIfAbsent(any(UserCardVO.class))).thenReturn(0);

		assertThat(cardRegistrationService.registerCards(USER_ID, List.of(card))).isZero();
		verify(cardMapper).insertUserCardIfAbsent(any(UserCardVO.class));
	}

	@Test
	@DisplayName("BenePay에 없는 상품 코드는 user_cards에 INSERT하지 않는다")
	void registerCardsSkipsUnknownProductCode() {
		KbCardResponseDto card = activeCard("card-ref-1", "unknown-product", "1111");
		when(cardMapper.existsPrimaryCardByUserId(USER_ID)).thenReturn(false);
		when(cardMapper.findCardIdByIssuerProductCode("unknown-product")).thenReturn(Optional.empty());

		assertThat(cardRegistrationService.registerCards(USER_ID, List.of(card))).isZero();
		verify(cardMapper).findCardIdByIssuerProductCode("unknown-product");
		verify(cardMapper, never()).insertUserCardIfAbsent(any(UserCardVO.class));
	}

	@Test
	@DisplayName("이미 대표 보유 카드가 있으면 신규 카드는 대표카드가 아니다")
	void registerCardsSetsNewCardNonPrimaryWhenPrimaryExists() {
		KbCardResponseDto card = activeCard("card-ref-1", "product-1", "1111");
		when(cardMapper.existsPrimaryCardByUserId(USER_ID)).thenReturn(true);
		when(cardMapper.findCardIdByIssuerProductCode("product-1")).thenReturn(Optional.of(CARD_ID));
		when(cardMapper.insertUserCardIfAbsent(any(UserCardVO.class))).thenReturn(1);

		cardRegistrationService.registerCards(USER_ID, List.of(card));

		ArgumentCaptor<UserCardVO> captor = ArgumentCaptor.forClass(UserCardVO.class);
		verify(cardMapper).insertUserCardIfAbsent(captor.capture());
		assertThat(captor.getValue().getPrimaryCard()).isFalse();
	}

	@Test
	@DisplayName("null 또는 빈 카드 목록은 Mapper를 호출하지 않고 0을 반환한다")
	void registerCardsReturnsZeroForNullOrEmptyList() {
		assertThat(cardRegistrationService.registerCards(USER_ID, null)).isZero();
		assertThat(cardRegistrationService.registerCards(USER_ID, List.of())).isZero();

		verify(cardMapper, never()).existsPrimaryCardByUserId(USER_ID);
	}

	private KbCardResponseDto activeCard(String referenceId, String productCode, String panLast4) {
		KbCardResponseDto card = new KbCardResponseDto();
		card.setCardReferenceId(referenceId);
		card.setProductCode(productCode);
		card.setTokenReferenceId("token-ref-" + referenceId);
		card.setPaymentToken("payment-token-" + referenceId);
		card.setCardExpiryYearMonth("202912");
		card.setTokenExpiryDate(LocalDate.of(2029, 12, 31));
		card.setPanLast4(panLast4);
		card.setCardStatus("ACTIVE");
		card.setTokenStatus("ACTIVE");
		return card;
	}
}
