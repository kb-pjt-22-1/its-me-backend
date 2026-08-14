package site.benepay.domain.recommendation.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * cards.benefits_info 원본 JSON 문자열을 {@link PerformanceTier} 목록으로 파싱한다.
 * 필드가 많고 선택적이라 전용 DTO 역직렬화 대신 {@link JsonNode} 순회를 쓴다 - 기존
 * RecommendationServiceImpl.findApplicableBenefit이 이미 이 스타일이라 맞춘 것.
 */
public final class BenefitJsonParser {

	private BenefitJsonParser() {
	}

	public static List<PerformanceTier> parse(String benefitsInfoJson, ObjectMapper objectMapper) {
		if (benefitsInfoJson == null || benefitsInfoJson.isBlank()) {
			return List.of();
		}

		try {
			JsonNode root = objectMapper.readTree(benefitsInfoJson);
			JsonNode tiersNode = root.path("performanceTiers");
			if (!tiersNode.isArray()) {
				return List.of();
			}

			List<PerformanceTier> tiers = new ArrayList<>();
			for (JsonNode tierNode : tiersNode) {
				tiers.add(parseTier(tierNode));
			}
			// benefits.py의 load_cards와 동일 - 구간을 최소실적 기준으로 정렬해 둔다.
			tiers.sort(Comparator.comparingLong(PerformanceTier::minimumSpending));
			return tiers;
		} catch (JsonProcessingException e) {
			// 카드 혜택 JSON 형식이 잘못된 카드는 혜택 없음으로 취급한다
			// (기존 RecommendationServiceImpl.findApplicableBenefit과 동일한 관용 처리).
			return List.of();
		}
	}

	private static PerformanceTier parseTier(JsonNode tierNode) {
		List<BenefitNode> benefits = new ArrayList<>();
		for (JsonNode benefitNode : tierNode.path("benefits")) {
			benefits.add(parseBenefit(benefitNode));
		}
		return new PerformanceTier(
			textOrNull(tierNode, "benefitNodeId"),
			textOrNull(tierNode, "tierName"),
			tierNode.path("minimumSpending").asLong(0L),
			nullableLong(tierNode, "maximumSpending"),
			nullableLong(tierNode, "maximumCombinedMonthlyBenefit"),
			textOrNull(tierNode, "monthlyLimitType"),
			benefits
		);
	}

	private static BenefitNode parseBenefit(JsonNode b) {
		return new BenefitNode(
			b.path("serviceName").asText(""),
			b.path("benefitType").asText(""),
			textArray(b.path("categoryCodes")),
			b.path("discountMethod").asText(""),
			b.path("discountRate").asDouble(0.0),
			b.path("discountAmount").asLong(0L),
			firstPresentLong(b, 0L, "weekdayDiscountPerLiter", "discountAmountPerLiter"),
			firstPresentLong(b, 0L, "weekendDiscountPerLiter", "discountAmountPerLiter"),
			b.path("minimumPaymentAmount").asLong(0L),
			firstPresentNullableLong(b, "maximumDiscountablePaymentAmount", "maximumPaymentAmountPerTransaction"),
			nullableLong(b, "maximumDiscountAmountPerTransaction"),
			firstPresentNullableLong(b, "maximumDiscountAmountPerMonth", "monthlyLimit"),
			firstPresentNullableLong(b, "maximumDiscountablePaymentAmountPerMonth", "maximumPaymentAmountPerMonth"),
			nullableInt(b, "monthlyCountLimit"),
			nullableInt(b, "annualCountLimit"),
			textArray(b.path("merchantNames")),
			b.path("integratedLimitExcluded").asBoolean(false),
			textOrNull(b, "merchantScope")
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return (value == null || value.isNull()) ? null : value.asText();
	}

	private static Long nullableLong(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return (value == null || value.isNull()) ? null : value.asLong();
	}

	private static Integer nullableInt(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return (value == null || value.isNull()) ? null : value.asInt();
	}

	private static List<String> textArray(JsonNode arrayNode) {
		List<String> values = new ArrayList<>();
		if (arrayNode.isArray()) {
			for (JsonNode value : arrayNode) {
				values.add(value.asText());
			}
		}
		return values;
	}

	/**
	 * 두 필드 중 먼저 존재하는(JSON에 실제로 있는) 값을 쓴다 - JSON 필드명이
	 * 리네이밍되며 신/구 명칭이 섞여 있는 경우 대응(benefits.py의 raw.get(A) or raw.get(B)).
	 */
	private static long firstPresentLong(JsonNode node, long fallback, String... fields) {
		for (String field : fields) {
			JsonNode value = node.get(field);
			if (value != null && !value.isNull()) {
				return value.asLong();
			}
		}
		return fallback;
	}

	private static Long firstPresentNullableLong(JsonNode node, String... fields) {
		for (String field : fields) {
			JsonNode value = node.get(field);
			if (value != null && !value.isNull()) {
				return value.asLong();
			}
		}
		return null;
	}
}
