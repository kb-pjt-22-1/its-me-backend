package site.benepay.domain.merchant.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;

@ExtendWith(MockitoExtension.class)
class MerchantCategoryControllerTest {

	@Mock
	private MerchantCategoryService merchantCategoryService;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new MerchantCategoryController(merchantCategoryService)).build();
	}

	@Test
	void getCategoryListReturnsOkWithBody() throws Exception {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder()
				.categoryCode("5812")
				.categoryName("음식점")
				.categoryIcon("icon-url")
				.build()));

		MvcResult result = mockMvc.perform(get("/api/v1/merchant-categories"))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		assertThat(body.get(0).get("categoryCode").asText()).isEqualTo("5812");
		assertThat(body.get(0).get("categoryName").asText()).isEqualTo("음식점");
	}

	@Test
	void getCategoryListReturnsEmptyArrayWhenNoneExist() throws Exception {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		MvcResult result = mockMvc.perform(get("/api/v1/merchant-categories"))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		assertThat(body.isArray()).isTrue();
		assertThat(body.size()).isZero();
	}
}
