package site.benepay.domain.merchant.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

import site.benepay.common.exception.DuplicateMerchantException;
import site.benepay.common.exception.GlobalExceptionHandler;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
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

	private String validRequestJson() throws Exception {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");
		return objectMapper.writeValueAsString(request);
	}

	private JsonNode bodyOf(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsByteArray());
	}

	// ---- GET /api/v1/merchants ----

	@Test
	void getMerchantListReturnsOkWithBody() throws Exception {
		when(merchantService.getMerchantList()).thenReturn(List.of(merchantResponse()));

		MvcResult result = mockMvc.perform(get("/api/v1/merchants"))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = bodyOf(result);
		assertThat(body.get(0).get("merchantId").asLong()).isEqualTo(MERCHANT_ID);
		assertThat(body.get(0).get("merchantCode").asText()).isEqualTo("M001");
	}

	// ---- GET /api/v1/merchants/nearby ----

	@Test
	void getNearbyMerchantsUsesDefaultRadiusWhenNotProvided() throws Exception {
		when(merchantService.getNearbyMerchants(37.5, 127.0, 1000)).thenReturn(
			List.of(NearbyMerchantResponseDto.builder().merchantId(MERCHANT_ID).distanceMeters(120.0).build()));

		MvcResult result = mockMvc.perform(get("/api/v1/merchants/nearby").param("lat", "37.5").param("lng", "127.0"))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(bodyOf(result).get(0).get("distanceMeters").asDouble()).isEqualTo(120.0);
		verify(merchantService).getNearbyMerchants(37.5, 127.0, 1000);
	}

	@Test
	void getNearbyMerchantsPassesExplicitRadius() throws Exception {
		when(merchantService.getNearbyMerchants(37.5, 127.0, 500)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/merchants/nearby")
				.param("lat", "37.5")
				.param("lng", "127.0")
				.param("radiusMeters", "500"))
			.andExpect(status().isOk());

		verify(merchantService).getNearbyMerchants(37.5, 127.0, 500);
	}

	// ---- GET /api/v1/merchants/within-bounds ----

	@Test
	void getMerchantsWithinBoundsReturnsOkWithBody() throws Exception {
		when(merchantService.getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1)).thenReturn(
			List.of(NearbyMerchantResponseDto.builder().merchantId(MERCHANT_ID).build()));

		MvcResult result = mockMvc.perform(get("/api/v1/merchants/within-bounds")
				.param("swLat", "37.4")
				.param("swLng", "126.9")
				.param("neLat", "37.6")
				.param("neLng", "127.1"))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(bodyOf(result).get(0).get("merchantId").asLong()).isEqualTo(MERCHANT_ID);
		verify(merchantService).getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1);
	}

	// ---- POST /api/v1/merchants ----

	@Test
	void createMerchantReturnsCreatedWithBody() throws Exception {
		when(merchantService.createMerchant(any(MerchantRequestDto.class))).thenReturn(merchantResponse());

		MvcResult result = mockMvc.perform(post("/api/v1/merchants")
				.contentType("application/json")
				.content(validRequestJson()))
			.andExpect(status().isCreated())
			.andReturn();

		assertThat(bodyOf(result).get("merchantCode").asText()).isEqualTo("M001");
	}

	@Test
	void createMerchantWithBlankMerchantNameReturnsBadRequest() throws Exception {
		MerchantRequestDto invalid = new MerchantRequestDto(
			"5812", 1L, "M001", "", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		mockMvc.perform(post("/api/v1/merchants")
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(invalid)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void createMerchantWithDuplicateMerchantCodeReturnsConflict() throws Exception {
		when(merchantService.createMerchant(any(MerchantRequestDto.class)))
			.thenThrow(new DuplicateMerchantException("이미 등록된 가맹점: M001"));

		mockMvc.perform(post("/api/v1/merchants")
				.contentType("application/json")
				.content(validRequestJson()))
			.andExpect(status().isConflict());
	}

	// ---- GET /api/v1/merchants/{merchantId} ----

	@Test
	void getMerchantReturnsOkWithBodyWhenFound() throws Exception {
		when(merchantService.getMerchant(MERCHANT_ID)).thenReturn(merchantResponse());

		MvcResult result = mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(bodyOf(result).get("merchantId").asLong()).isEqualTo(MERCHANT_ID);
	}

	@Test
	void getMerchantReturnsNotFoundWhenMissing() throws Exception {
		when(merchantService.getMerchant(MERCHANT_ID)).thenThrow(
			new MerchantNotFoundException("가맹점을 찾을 수 없음: " + MERCHANT_ID));

		mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID))
			.andExpect(status().isNotFound());
	}

	// ---- PUT /api/v1/merchants/{merchantId} ----

	@Test
	void updateMerchantReturnsOkWithBody() throws Exception {
		when(merchantService.updateMerchant(eq(MERCHANT_ID), any(MerchantRequestDto.class))).thenReturn(
			merchantResponse());

		MvcResult result = mockMvc.perform(put("/api/v1/merchants/{merchantId}", MERCHANT_ID)
				.contentType("application/json")
				.content(validRequestJson()))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(bodyOf(result).get("merchantCode").asText()).isEqualTo("M001");
	}

	@Test
	void updateMerchantReturnsNotFoundWhenMissing() throws Exception {
		when(merchantService.updateMerchant(eq(MERCHANT_ID), any(MerchantRequestDto.class)))
			.thenThrow(new MerchantNotFoundException("가맹점을 찾을 수 없음: " + MERCHANT_ID));

		mockMvc.perform(put("/api/v1/merchants/{merchantId}", MERCHANT_ID)
				.contentType("application/json")
				.content(validRequestJson()))
			.andExpect(status().isNotFound());
	}

	// ---- DELETE /api/v1/merchants/{merchantId} ----

	@Test
	void deleteMerchantReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/merchants/{merchantId}", MERCHANT_ID))
			.andExpect(status().isNoContent());

		verify(merchantService).deleteMerchant(MERCHANT_ID);
	}

	@Test
	void deleteMerchantReturnsNotFoundWhenMissing() throws Exception {
		doThrow(new MerchantNotFoundException("가맹점을 찾을 수 없음: " + MERCHANT_ID))
			.when(merchantService).deleteMerchant(MERCHANT_ID);

		mockMvc.perform(delete("/api/v1/merchants/{merchantId}", MERCHANT_ID))
			.andExpect(status().isNotFound());
	}
}
