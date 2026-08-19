package site.benepay.domain.merchant.vo;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MerchantTest {

	private Merchant.MerchantBuilder validBuilder() {
		return Merchant.builder()
			.merchantId(1L)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode("M001")
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000");
	}

	@Test
	void builderCreatesMerchantWithAllFields() {
		Merchant merchant = validBuilder().build();

		assertThat(merchant.getMerchantId()).isEqualTo(1L);
		assertThat(merchant.getCategoryCode()).isEqualTo("5812");
		assertThat(merchant.getBrandId()).isEqualTo(1L);
		assertThat(merchant.getMerchantCode()).isEqualTo("M001");
		assertThat(merchant.getMerchantName()).isEqualTo("테스트 식당");
		assertThat(merchant.getAddress()).isEqualTo("서울시 강남구");
		assertThat(merchant.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(37.5));
		assertThat(merchant.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(127.0));
		assertThat(merchant.getPhone()).isEqualTo("02-000-0000");
	}

	@Test
	void builderAllowsNullMerchantIdAndPhone() {
		Merchant merchant = validBuilder().merchantId(null).phone(null).build();

		assertThat(merchant.getMerchantId()).isNull();
		assertThat(merchant.getPhone()).isNull();
	}

	@Test
	void builderRejectsNullCategoryCode() {
		Merchant.MerchantBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.categoryCode(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void builderRejectsNullBrandId() {
		Merchant.MerchantBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.brandId(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void builderRejectsNullMerchantCode() {
		Merchant.MerchantBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.merchantCode(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void builderRejectsNullMerchantName() {
		Merchant.MerchantBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.merchantName(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void builderRejectsNullAddress() {
		Merchant.MerchantBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.address(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void builderRejectsNullLatitude() {
		Merchant.MerchantBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.latitude(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void builderRejectsNullLongitude() {
		Merchant.MerchantBuilder builder = validBuilder();

		assertThatThrownBy(() -> builder.longitude(null))
			.isInstanceOf(NullPointerException.class);
	}

}
