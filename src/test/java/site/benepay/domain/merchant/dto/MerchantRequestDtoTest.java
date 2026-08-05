package site.benepay.domain.merchant.dto;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MerchantRequestDtoTest {

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
	void validRequestHasNoViolations() {
		assertThat(propertyPaths(validRequest())).isEmpty();
	}

	@Test
	void requestWithoutPhoneHasNoViolations() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), null);

		assertThat(propertyPaths(request)).isEmpty();
	}

	@Test
	void blankCategoryCodeIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"", 1L, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("categoryCode");
	}

	@Test
	void categoryCodeMustBeFourDigits() {
		MerchantRequestDto request = new MerchantRequestDto(
			"581", 1L, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("categoryCode");
	}

	@Test
	void nullBrandIdIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", null, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("brandId");
	}

	@Test
	void blankMerchantCodeIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("merchantCode");
	}

	@Test
	void merchantCodeLongerThan50CharsIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M".repeat(51), "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("merchantCode");
	}

	@Test
	void blankMerchantNameIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M001", "", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("merchantName");
	}

	@Test
	void blankAddressIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M001", "테스트 식당", "",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("address");
	}

	@Test
	void nullLatitudeIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M001", "테스트 식당", "서울시 강남구",
			null, BigDecimal.valueOf(127.0), "02-000-0000");

		assertThat(propertyPaths(request)).contains("latitude");
	}

	@Test
	void nullLongitudeIsRejected() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), null, "02-000-0000");

		assertThat(propertyPaths(request)).contains("longitude");
	}

	@Test
	void malformedPhoneIsRejectedWithMessage() {
		MerchantRequestDto request = new MerchantRequestDto(
			"5812", 1L, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "not-a-phone");

		Set<ConstraintViolation<MerchantRequestDto>> violations = validator.validate(request);

		assertThat(violations)
			.filteredOn(v -> v.getPropertyPath().toString().equals("phone"))
			.extracting(ConstraintViolation::getMessage)
			.containsExactly("잘못된 전화번호 형식입니다.");
	}

	private MerchantRequestDto validRequest() {
		return new MerchantRequestDto(
			"5812", 1L, "M001", "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");
	}

	private Set<String> propertyPaths(MerchantRequestDto request) {
		return validator.validate(request).stream()
			.map(v -> v.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}
}
