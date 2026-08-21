package site.benepay.domain.merchant.dto;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import site.benepay.domain.merchant.vo.Merchant;

class MerchantResponseDtoTest {

	@Test
	void fromMapsEveryFieldFromMerchant() {
		Merchant merchant = Merchant.builder()
			.merchantId(7L)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode("M001")
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.build();

		MerchantResponseDto response = MerchantResponseDto.from(merchant);

		assertThat(response.getMerchantId()).isEqualTo(7L);
		assertThat(response.getCategoryCode()).isEqualTo("5812");
		assertThat(response.getBrandId()).isEqualTo(1L);
		assertThat(response.getMerchantCode()).isEqualTo("M001");
		assertThat(response.getMerchantName()).isEqualTo("테스트 식당");
		assertThat(response.getAddress()).isEqualTo("서울시 강남구");
		assertThat(response.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(37.5));
		assertThat(response.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(127.0));
	}
}
