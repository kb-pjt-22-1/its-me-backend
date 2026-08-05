package site.benepay.domain.merchant.vo;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantCategoryTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void constructorRejectsNullCategoryCode() {
        assertThatThrownBy(() -> new MerchantCategory(null, "음식점", "icon-url"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullCategoryName() {
        assertThatThrownBy(() -> new MerchantCategory("5812", null, "icon-url"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullCategoryIcon() {
        assertThatThrownBy(() -> new MerchantCategory("5812", "음식점", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validCategoryCodeHasNoViolations() {
        MerchantCategory category = new MerchantCategory("5812", "음식점", "icon-url");

        Set<ConstraintViolation<MerchantCategory>> violations = validator.validate(category);

        assertThat(violations).isEmpty();
    }

    @Test
    void categoryCodeMustBeFourDigits() {
        MerchantCategory category = new MerchantCategory("abc", "음식점", "icon-url");

        Set<ConstraintViolation<MerchantCategory>> violations = validator.validate(category);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("categoryCode");
    }
}
