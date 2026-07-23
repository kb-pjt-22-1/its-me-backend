package site.benepay.domain.user.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import site.benepay.common.exception.InvalidPinFormatException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"481027", "592841", "306192"})
    void validPinsPassWithoutException(String pin) {
        assertThatCode(() -> PinValidator.validate(pin)).doesNotThrowAnyException();
    }

    @Test
    void nullPinIsRejected() {
        assertThatThrownBy(() -> PinValidator.validate(null))
                .isInstanceOf(InvalidPinFormatException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "12a456", "", "123 45"})
    void nonSixDigitPinsAreRejected(String pin) {
        assertThatThrownBy(() -> PinValidator.validate(pin))
                .isInstanceOf(InvalidPinFormatException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"111234", "234111", "560111"})
    void threeOrMoreRepeatingDigitsAreRejected(String pin) {
        assertThatThrownBy(() -> PinValidator.validate(pin))
                .isInstanceOf(InvalidPinFormatException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "912345", "456789"})
    void threeOrMoreAscendingDigitsAreRejected(String pin) {
        assertThatThrownBy(() -> PinValidator.validate(pin))
                .isInstanceOf(InvalidPinFormatException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"654321", "987654", "321098"})
    void threeOrMoreDescendingDigitsAreRejected(String pin) {
        assertThatThrownBy(() -> PinValidator.validate(pin))
                .isInstanceOf(InvalidPinFormatException.class);
    }
}
