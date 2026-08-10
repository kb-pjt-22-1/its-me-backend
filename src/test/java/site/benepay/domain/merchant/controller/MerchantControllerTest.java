package site.benepay.domain.merchant.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
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

import site.benepay.common.exception.GlobalExceptionHandler;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;

@ExtendWith(MockitoExtension.class)
class MerchantControllerTest {

	private static final Long MERCHANT_ID = 7L;

	@Mock
	private MerchantService merchantService;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new MerchantController(merchantService))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	private MerchantResponseDto merchantResponse() {
		return MerchantResponseDto.builder()
			.merchantId(MERCHANT_ID)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode("M001")
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000")
			.build();
	}

	private JsonNode bodyOf(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsByteArray());
	}

	// ---- GET /api/v1/merchants ----

	@Test
	void getMerchantsReturnsOkWithBody() throws Exception {
		when(merchantService.getMerchants(null)).thenReturn(List.of(merchantResponse()));

		MvcResult result = mockMvc.perform(get("/api/v1/merchants"))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = bodyOf(result);
		assertThat(body.get(0).get("merchantId").asLong()).isEqualTo(MERCHANT_ID);
		assertThat(body.get(0).get("merchantCode").asText()).isEqualTo("M001");
	}

	@Test
	void getMerchantsPassesCategoryCodeWhenProvided() throws Exception {
		when(merchantService.getMerchants("5812")).thenReturn(List.of(merchantResponse()));

		mockMvc.perform(get("/api/v1/merchants").param("categoryCode", "5812"))
			.andExpect(status().isOk());

		verify(merchantService).getMerchants("5812");
	}

	// ---- GET /api/v1/merchants/{merchantId} ----

	@Test
	void getMerchantReturnsOkWithBody() throws Exception {
		when(merchantService.getMerchant(MERCHANT_ID)).thenReturn(merchantResponse());

		MvcResult result = mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = bodyOf(result);
		assertThat(body.get("merchantId").asLong()).isEqualTo(MERCHANT_ID);
	}

	@Test
	void getMerchantReturnsNotFoundWhenMissing() throws Exception {
		when(merchantService.getMerchant(MERCHANT_ID)).thenThrow(new MerchantNotFoundException("존재하지 않는 매장입니다: " + MERCHANT_ID));

		mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID))
			.andExpect(status().isNotFound());
	}
}
