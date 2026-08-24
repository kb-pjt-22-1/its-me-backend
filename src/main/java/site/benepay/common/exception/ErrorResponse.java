package site.benepay.common.exception;

import java.time.LocalDateTime;
import java.time.ZoneId;

public record ErrorResponse(
	int status,
	String message,
	String path,
	LocalDateTime timestamp
) {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	public static ErrorResponse of(int status, String message, String path) {
		return new ErrorResponse(status, message, path, LocalDateTime.now(ZONE));
	}
}
