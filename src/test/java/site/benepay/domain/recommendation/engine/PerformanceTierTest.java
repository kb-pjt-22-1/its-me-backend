package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PerformanceTierTest {

	@Test
	void combinedCapIsNullWhenMonthlyLimitTypeExcludesIt() {
		PerformanceTier sumOfIndividual =
			new PerformanceTier(null, "1구간", 0, null, 500L, "SUM_OF_INDIVIDUAL_LIMITS", List.of());
		PerformanceTier unlimited = new PerformanceTier(null, "1구간", 0, null, 500L, "UNLIMITED", List.of());

		assertThat(sumOfIndividual.combinedCap()).isNull();
		assertThat(unlimited.combinedCap()).isNull();
	}

	@Test
	void combinedCapReturnsTheConfiguredCapForOtherLimitTypes() {
		PerformanceTier integrated = new PerformanceTier(null, "1구간", 0, null, 500L, "INTEGRATED_LIMIT", List.of());
		PerformanceTier noType = new PerformanceTier(null, "1구간", 0, null, 500L, null, List.of());

		assertThat(integrated.combinedCap()).isEqualTo(500L);
		assertThat(noType.combinedCap()).isEqualTo(500L);
	}
}
