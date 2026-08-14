package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import org.junit.jupiter.api.Test;

import site.benepay.domain.recommendation.engine.RecommendationParams.TicketHistogram;

class RecommendationParamsTest {

	@Test
	void passRateReturnsShareOfCountsAtOrAboveThreshold() {
		// centers 10,000/20,000/30,000원 구간에 각각 10/10/10건 - 3만원 이상은 10/30.
		TicketHistogram histogram = new TicketHistogram(
			new double[] {10_000, 20_000, 30_000},
			Map.of("카페", new long[] {10L, 10L, 10L})
		);

		assertThat(histogram.passRate("카페", 30_000)).isCloseTo(10.0 / 30.0, within(1e-9));
		assertThat(histogram.passRate("카페", 20_000)).isCloseTo(20.0 / 30.0, within(1e-9));
		assertThat(histogram.passRate("카페", 0)).isCloseTo(1.0, within(1e-9));
	}

	@Test
	void passRateReturnsOneWhenMajorIsNotInHistogram() {
		TicketHistogram histogram = new TicketHistogram(
			new double[] {10_000},
			Map.of("카페", new long[] {10L})
		);

		assertThat(histogram.passRate("존재하지않는대분류", 10_000)).isEqualTo(1.0);
	}

	@Test
	void passRateReturnsOneWhenTotalCountIsZero() {
		TicketHistogram histogram = new TicketHistogram(
			new double[] {10_000},
			Map.of("카페", new long[] {0L})
		);

		assertThat(histogram.passRate("카페", 10_000)).isEqualTo(1.0);
	}

	@Test
	void equalsAndHashCodeCompareArrayContentNotReference() {
		TicketHistogram a = new TicketHistogram(new double[] {1.0, 2.0}, Map.of("카페", new long[] {1L}));
		TicketHistogram b = new TicketHistogram(new double[] {1.0, 2.0}, Map.of("카페", new long[] {1L}));
		TicketHistogram different = new TicketHistogram(new double[] {9.0}, Map.of("카페", new long[] {1L}));

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
		assertThat(a).isNotEqualTo(different);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("문자열");
		assertThat(a).isEqualTo(a);
	}

	@Test
	void equalsReturnsFalseWhenCountsValuesDifferForTheSameKey() {
		// centers/키셋은 같지만 같은 키의 long[] 내용이 달라 항목 단위 비교에서 걸러져야 한다.
		TicketHistogram a = new TicketHistogram(new double[] {1.0}, Map.of("카페", new long[] {1L, 2L}));
		TicketHistogram sameKeyDifferentValues =
			new TicketHistogram(new double[] {1.0}, Map.of("카페", new long[] {9L, 9L}));

		assertThat(a).isNotEqualTo(sameKeyDifferentValues);
	}

	@Test
	void toStringIncludesArrayContentNotDefaultArrayRepresentation() {
		TicketHistogram histogram = new TicketHistogram(new double[] {1.0, 2.0}, Map.of("카페", new long[] {5L}));

		assertThat(histogram.toString()).contains("1.0").contains("2.0").doesNotContain("[D@");
	}
}
