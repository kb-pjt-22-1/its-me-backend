package site.benepay.common.util;

import site.benepay.common.exception.InvalidCoordinateException;

public final class GeoCoordinateValidator {

	private static final double MIN_LATITUDE = -90.0;
	private static final double MAX_LATITUDE = 90.0;
	private static final double MIN_LONGITUDE = -180.0;
	private static final double MAX_LONGITUDE = 180.0;

	private GeoCoordinateValidator() {
	}

	public static void validate(double latitude, double longitude) {
		if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE
			|| longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
			throw new InvalidCoordinateException(
				"위도/경도 범위를 벗어났습니다: latitude=" + latitude + ", longitude=" + longitude);
		}
	}
}
