package site.benepay.common.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import site.benepay.common.exception.InvalidCoordinateException;

class GeoCoordinateValidatorTest {

	@Test
	void acceptsBoundaryValues() {
		assertThatCode(() -> GeoCoordinateValidator.validate(90.0, 180.0)).doesNotThrowAnyException();
		assertThatCode(() -> GeoCoordinateValidator.validate(-90.0, -180.0)).doesNotThrowAnyException();
	}

	@Test
	void rejectsLatitudeAboveMax() {
		assertThatThrownBy(() -> GeoCoordinateValidator.validate(90.1, 0.0))
			.isInstanceOf(InvalidCoordinateException.class);
	}

	@Test
	void rejectsLatitudeBelowMin() {
		assertThatThrownBy(() -> GeoCoordinateValidator.validate(-90.1, 0.0))
			.isInstanceOf(InvalidCoordinateException.class);
	}

	@Test
	void rejectsLongitudeAboveMax() {
		assertThatThrownBy(() -> GeoCoordinateValidator.validate(0.0, 180.1))
			.isInstanceOf(InvalidCoordinateException.class);
	}

	@Test
	void rejectsLongitudeBelowMin() {
		assertThatThrownBy(() -> GeoCoordinateValidator.validate(0.0, -180.1))
			.isInstanceOf(InvalidCoordinateException.class);
	}
}
