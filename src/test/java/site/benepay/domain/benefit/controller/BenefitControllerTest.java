package site.benepay.domain.benefit.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.service.BenefitService;

@ExtendWith(MockitoExtension.class)
class BenefitControllerTest {

	private static final Long USER_ID = 1L;
	private static final int BASE_YEAR = 2025;

	@Mock
	private BenefitService benefitService;

	private BenefitController controller;

	@BeforeEach
	void setUp() {
		controller = new BenefitController(benefitService);
	}

	@Test
	void getAnnualFeeBreakEvenUsesTheGivenYear() {
		List<AnnualFeeBreakEvenResponseDto> serviceResult =
			List.of(
				AnnualFeeBreakEvenResponseDto.builder()
					.userCardId(100L)
					.baseYear(BASE_YEAR)
					.annualFee(10_000L)
					.accumulatedBenefit(12_000L)
					.build()
			);

		when(
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			)
		).thenReturn(serviceResult);

		ResponseEntity<List<AnnualFeeBreakEvenResponseDto>>
			response =
			controller.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			);

		assertThat(response.getStatusCode())
			.isEqualTo(HttpStatus.OK);
		assertThat(response.getBody())
			.isEqualTo(serviceResult);

		verify(benefitService)
			.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			);
	}

	@Test
	void getAnnualFeeBreakEvenDefaultsToTheCurrentYear() {
		int currentYear =
			Year.now().getValue();

		List<AnnualFeeBreakEvenResponseDto> serviceResult =
			List.of();

		when(
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				currentYear
			)
		).thenReturn(serviceResult);

		ResponseEntity<List<AnnualFeeBreakEvenResponseDto>>
			response =
			controller.getAnnualFeeBreakEven(
				USER_ID,
				null
			);

		assertThat(response.getStatusCode())
			.isEqualTo(HttpStatus.OK);
		assertThat(response.getBody())
			.isEmpty();

		verify(benefitService)
			.getAnnualFeeBreakEven(
				USER_ID,
				currentYear
			);
	}
}