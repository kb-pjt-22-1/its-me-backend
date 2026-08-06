package site.benepay.domain.merchant.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import site.benepay.domain.merchant.vo.MerchantBrand;

class MerchantBrandResponseDtoTest {

	@Test
	void fromMapsEveryFieldFromMerchantBrand() {
		MerchantBrand brand = MerchantBrand.builder()
			.brandId(1L)
			.brandCode("B001")
			.brandName("브랜드A")
			.brandLogo("logo-url")
			.build();
		MerchantBrandResponseDto response = MerchantBrandResponseDto.from(brand);

		assertThat(response.getBrandId()).isEqualTo(1L);
		assertThat(response.getBrandCode()).isEqualTo("B001");
		assertThat(response.getBrandName()).isEqualTo("브랜드A");
		assertThat(response.getBrandLogo()).isEqualTo("logo-url");
	}
}
