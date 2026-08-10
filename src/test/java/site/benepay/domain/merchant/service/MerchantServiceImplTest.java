package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;
import site.benepay.domain.recommendation.service.RecommendationService;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

	private static final Long MERCHANT_ID = 7L;
	private static final Long USER_ID = 1L;

	@Mock
	private MerchantMapper merchantMapper;

	@Mock
	private RecommendationService recommendationService;

	private MerchantServiceImpl merchantService;

	@BeforeEach
	void setUp() {
		merchantService = new MerchantServiceImpl(merchantMapper, recommendationService);
	}

	private Merchant existingMerchant(String merchantCode) {
		return Merchant.builder()
			.merchantId(MERCHANT_ID)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode(merchantCode)
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000")
			.build();
	}

	// ---- getMerchants(categoryCode) ----

	@Test
	void getMerchantsMapsEveryMerchantToADto() {
		when(merchantMapper.findAll(null)).thenReturn(List.of(existingMerchant("M001"), existingMerchant("M002")));

		List<MerchantResponseDto> result = merchantService.getMerchants(null);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(MerchantResponseDto::getMerchantCode).containsExactly("M001", "M002");
	}

	@Test
	void getMerchantsReturnsEmptyListWhenNoneExist() {
		when(merchantMapper.findAll(null)).thenReturn(List.of());

		assertThat(merchantService.getMerchants(null)).isEmpty();
	}

	@Test
	void getMerchantsPassesCategoryCodeThroughToMapper() {
		when(merchantMapper.findAll("5812")).thenReturn(List.of(existingMerchant("M001")));

		List<MerchantResponseDto> result = merchantService.getMerchants("5812");

		assertThat(result).hasSize(1);
		verify(merchantMapper).findAll("5812");
	}

	// ---- getMerchant(merchantId) ----

	@Test
	void getMerchantReturnsDtoWhenFound() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(existingMerchant("M001")));

		MerchantResponseDto result = merchantService.getMerchant(MERCHANT_ID);

		assertThat(result.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(result.getMerchantCode()).isEqualTo("M001");
	}

	@Test
	void getMerchantThrowsWhenNotFound() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> merchantService.getMerchant(MERCHANT_ID))
			.isInstanceOf(MerchantNotFoundException.class);
	}

	// ---- getRecommendedMerchantsInBounds(userId, bounds, categoryCode) ----

	@Test
	void getRecommendedMerchantsInBoundsSendsCandidatesToRecommendationServiceAndReturnsItsResult() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, null))
			.thenReturn(List.of(existingMerchant("M001")));

		List<MerchantRecommendationResponseDto> marked =
			List.of(MerchantRecommendationResponseDto.from(existingMerchant("M001")).toBuilder()
				.recommended(true)
				.build());
		when(recommendationService.markRecommendedMerchants(eq(USER_ID), anyList())).thenReturn(marked);

		List<MerchantRecommendationResponseDto> result =
			merchantService.getRecommendedMerchantsInBounds(USER_ID, 37.4, 127.0, 37.6, 127.2, null);

		assertThat(result).isEqualTo(marked);
		verify(recommendationService).markRecommendedMerchants(eq(USER_ID), argThat(candidates ->
			candidates.size() == 1 && !candidates.get(0).isRecommended()
		));
	}

	@Test
	void getRecommendedMerchantsInBoundsSkipsRecommendationServiceWhenNoCandidates() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, null)).thenReturn(List.of());

		List<MerchantRecommendationResponseDto> result =
			merchantService.getRecommendedMerchantsInBounds(USER_ID, 37.4, 127.0, 37.6, 127.2, null);

		assertThat(result).isEmpty();
		verifyNoInteractions(recommendationService);
	}
}
