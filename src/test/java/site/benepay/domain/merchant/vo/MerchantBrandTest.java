package site.benepay.domain.merchant.vo;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MerchantBrandTest {

	private MerchantBrand.MerchantBrandBuilder validBuilder() {
		return MerchantBrand.builder()
			.brandId(1L)
			.brandCode("B001")
			.brandName("브랜드A")
			.brandLogo("logo-url");
	}

	@Test
	void builderCreatesMerchantBrandWithAllFields() {
		MerchantBrand brand = validBuilder().build();

		assertThat(brand.getBrandId()).isEqualTo(1L);
		assertThat(brand.getBrandCode()).isEqualTo("B001");
		assertThat(brand.getBrandName()).isEqualTo("브랜드A");
		assertThat(brand.getBrandLogo()).isEqualTo("logo-url");
	}

	@Test
	void builderAllowsNullBrandIdAndBrandLogo() {
		MerchantBrand brand = validBuilder().brandId(null).brandLogo(null).build();

		assertThat(brand.getBrandId()).isNull();
		assertThat(brand.getBrandLogo()).isNull();
	}

	@Test
	void builderRejectsNullBrandCode() {
		MerchantBrand.MerchantBrandBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.brandCode(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void builderRejectsNullBrandName() {
		MerchantBrand.MerchantBrandBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.brandName(null))
			.isInstanceOf(NullPointerException.class);
	}
}
