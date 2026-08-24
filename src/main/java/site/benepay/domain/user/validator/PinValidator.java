package site.benepay.domain.user.validator;

import java.util.regex.Pattern;

import site.benepay.common.exception.InvalidPinFormatException;

public final class PinValidator {

	private static final Pattern SIX_DIGITS = Pattern.compile("^\\d{6}$");

	private PinValidator() {
	}

	public static void validate(String pin) {
		if (pin == null || !SIX_DIGITS.matcher(pin).matches()) {
			throw new InvalidPinFormatException("PIN은 정확히 6자리 숫자여야 합니다.");
		}
		if (hasThreeOrMoreConsecutiveRepeatingOrSequentialDigits(pin)) {
			throw new InvalidPinFormatException("PIN에 3자리 이상 반복되거나 연속된 숫자를 포함할 수 없습니다.");
		}
	}

	private static boolean hasThreeOrMoreConsecutiveRepeatingOrSequentialDigits(String pin) {
		for (int i = 0; i <= pin.length() - 3; i++) {
			int a = pin.charAt(i) - '0';
			int b = pin.charAt(i + 1) - '0';
			int c = pin.charAt(i + 2) - '0';
			boolean repeating = a == b && b == c;
			boolean ascending = b == a + 1 && c == b + 1;
			boolean descending = b == a - 1 && c == b - 1;
			if (repeating || ascending || descending) {
				return true;
			}
		}
		return false;
	}
}
