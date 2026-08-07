package site.benepay.domain.user.validator;

import java.util.regex.Pattern;

import site.benepay.common.exception.InvalidPinFormatException;

public final class PinValidator {

	private static final Pattern SIX_DIGITS = Pattern.compile("^\\d{6}$");

	private PinValidator() {
	}

	public static void validate(String pin) {
		if (pin == null || !SIX_DIGITS.matcher(pin).matches()) {
			throw new InvalidPinFormatException("PIN must be exactly 6 digits");
		}
		if (hasThreeOrMoreConsecutiveRepeatingOrSequentialDigits(pin)) {
			throw new InvalidPinFormatException("PIN must not contain 3 or more repeating or sequential digits");
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
