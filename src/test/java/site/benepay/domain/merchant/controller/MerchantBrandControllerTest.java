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

import site.benepay.domain.merchant.dto.MerchantBrandResponseDto;
import site.benepay.domain.merchant.service.MerchantBrandService;

@ExtendWith(MockitoExtension.class)
class MerchantBrandControllerTest {

	@Mock
	private MerchantBrandService merchantBrandService;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new MerchantBrandController(merchantBrandService)).build();
	}

	@Test
	void getBrandListReturnsOkWithBody() throws Exception {
		when(merchantBrandService.getBrandList()).thenReturn(List.of(
			MerchantBrandResponseDto.builder()
				.brandId(1L)
				.brandCode("B001")
				.brandName("브랜드A")
				.brandLogo("logo-url")
				.build()));

		MvcResult result = mockMvc.perform(get("/api/v1/merchant-brands"))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		assertThat(body.get(0).get("brandId").asLong()).isEqualTo(1L);
		assertThat(body.get(0).get("brandName").asText()).isEqualTo("브랜드A");
	}

	@Test
	void getBrandListReturnsEmptyArrayWhenNoneExist() throws Exception {
		when(merchantBrandService.getBrandList()).thenReturn(List.of());

		MvcResult result = mockMvc.perform(get("/api/v1/merchant-brands"))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		assertThat(body.isArray()).isTrue();
		assertThat(body.size()).isZero();
	}
}
