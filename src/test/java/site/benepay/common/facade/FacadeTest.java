package site.benepay.common.facade;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;

class FacadeTest {

	private static final Long USER_ID = 1L;

	private final Facade facade = new Facade();

	@Test
	void getRecommendedMerchantsMapsEveryMerchantWithoutMarkingAnyAsRecommendedYet() {
		MerchantResponseDto merchant = MerchantResponseDto.builder()
			.merchantId(7L)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode("M001")
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000")
			.build();

		List<MerchantRecommendationResponseDto> result =
			facade.getRecommendedMerchants(USER_ID, List.of(merchant));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantId()).isEqualTo(merchant.getMerchantId());
		assertThat(result.get(0).getMerchantCode()).isEqualTo(merchant.getMerchantCode());
		assertThat(result.get(0).isRecommended()).isFalse();
	}

	@Test
	void getRecommendedMerchantsReturnsEmptyListForEmptyInput() {
		assertThat(facade.getRecommendedMerchants(USER_ID, List.of())).isEmpty();
	}
}
