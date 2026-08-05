package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.mapper.MerchantCategoryMapper;
import site.benepay.domain.merchant.vo.MerchantCategory;

@ExtendWith(MockitoExtension.class)
class MerchantCategoryServiceImplTest {

	@Mock
	private MerchantCategoryMapper merchantCategoryMapper;

	private MerchantCategoryServiceImpl merchantCategoryService;

	@BeforeEach
	void setUp() {
		merchantCategoryService = new MerchantCategoryServiceImpl(merchantCategoryMapper);
	}

	@Test
	void getCategoryListMapsEveryCategoryToADto() {
		when(merchantCategoryMapper.findAll()).thenReturn(List.of(
			new MerchantCategory("5812", "음식점", "icon-restaurant"),
			new MerchantCategory("5813", "카페", "icon-cafe")));

		List<MerchantCategoryResponseDto> result = merchantCategoryService.getCategoryList();

		assertThat(result).hasSize(2);
		assertThat(result).extracting(MerchantCategoryResponseDto::getCategoryCode).containsExactly("5812", "5813");
		assertThat(result).extracting(MerchantCategoryResponseDto::getCategoryName).containsExactly("음식점", "카페");
	}

	@Test
	void getCategoryListReturnsEmptyListWhenNoneExist() {
		when(merchantCategoryMapper.findAll()).thenReturn(List.of());

		assertThat(merchantCategoryService.getCategoryList()).isEmpty();
	}
}
