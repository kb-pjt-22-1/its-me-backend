package site.benepay.domain.merchant.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import site.benepay.domain.merchant.vo.MerchantCategory;

class MerchantCategoryResponseDtoTest {

	@Test
	void fromMapsEveryFieldFromMerchantCategory() {
		MerchantCategory category = new MerchantCategory("5812", "음식점", "icon-url");

		MerchantCategoryResponseDto response = MerchantCategoryResponseDto.from(category);
		assertThat(response.getCategoryCode()).isEqualTo("5812");
		assertThat(response.getCategoryName()).isEqualTo("음식점");
		assertThat(response.getCategoryIcon()).isEqualTo("icon-url");
	}
}
