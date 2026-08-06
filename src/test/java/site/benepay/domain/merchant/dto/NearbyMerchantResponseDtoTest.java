package site.benepay.domain.merchant.dto;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import site.benepay.domain.merchant.vo.MerchantNearbyVO;

class NearbyMerchantResponseDtoTest {

	@Test
	void fromMapsEveryFieldIncludingDistance() {
		MerchantNearbyVO nearby = mock(MerchantNearbyVO.class);
		when(nearby.getMerchantId()).thenReturn(7L);
		when(nearby.getCategoryCode()).thenReturn("5812");
		when(nearby.getBrandId()).thenReturn(1L);
		when(nearby.getMerchantCode()).thenReturn("M001");
		when(nearby.getMerchantName()).thenReturn("테스트 식당");
		when(nearby.getAddress()).thenReturn("서울시 강남구");
		when(nearby.getLatitude()).thenReturn(BigDecimal.valueOf(37.5));
		when(nearby.getLongitude()).thenReturn(BigDecimal.valueOf(127.0));
		when(nearby.getPhone()).thenReturn("02-000-0000");
		when(nearby.getDistanceMeters()).thenReturn(120.5);

		NearbyMerchantResponseDto response = NearbyMerchantResponseDto.from(nearby);

		assertThat(response.getMerchantId()).isEqualTo(7L);
		assertThat(response.getCategoryCode()).isEqualTo("5812");
		assertThat(response.getBrandId()).isEqualTo(1L);
		assertThat(response.getMerchantCode()).isEqualTo("M001");
		assertThat(response.getMerchantName()).isEqualTo("테스트 식당");
		assertThat(response.getAddress()).isEqualTo("서울시 강남구");
		assertThat(response.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(37.5));
		assertThat(response.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(127.0));
		assertThat(response.getPhone()).isEqualTo("02-000-0000");
		assertThat(response.getDistanceMeters()).isEqualTo(120.5);
	}
}
