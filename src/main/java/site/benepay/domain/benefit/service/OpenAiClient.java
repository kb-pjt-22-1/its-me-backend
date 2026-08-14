package site.benepay.domain.benefit.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.domain.benefit.dto.BenefitCoachDataDto.CalculatedCoachingData;

@Component
public class OpenAiClient {

	private static final String FIELD_CONTENT = "content";
	private static final String FIELD_INDEX = "index";
	private static final String FIELD_ITEMS = "items";
	private static final String FIELD_MESSAGE = "message";
	private static final String FIELD_SUMMARY = "summary";
	private static final String FIELD_TITLE = "title";
	private static final String JSON_TYPE_STRING = "string";

	private final String systemPrompt;

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;
	private final String apiKey;
	private final String apiUrl;
	private final String model;

	public OpenAiClient(
		ObjectMapper objectMapper,
		@Value("${openai.api-key}") String apiKey,
		@Value("${openai.api-url}") String apiUrl,
		@Value("${openai.model}") String model,
		@Value("${openai.connect-timeout-ms}") int connectTimeoutMs,
		@Value("${openai.read-timeout-ms}") int readTimeoutMs,
		@Value("classpath:ai/benefit-coach-system-prompt.txt")
		Resource systemPromptResource
	) {
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.apiUrl = apiUrl;
		this.model = model;
		this.systemPrompt =
			loadSystemPrompt(systemPromptResource);

		SimpleClientHttpRequestFactory requestFactory =
			new SimpleClientHttpRequestFactory();

		requestFactory.setConnectTimeout(connectTimeoutMs);
		requestFactory.setReadTimeout(readTimeoutMs);

		this.restTemplate =
			new RestTemplate(requestFactory);
	}

	private String loadSystemPrompt(
		Resource systemPromptResource
	) {
		try (
			InputStream inputStream =
				systemPromptResource.getInputStream()
		) {
			String prompt =
				StreamUtils.copyToString(
					inputStream,
					StandardCharsets.UTF_8
				).trim();

			if (prompt.isBlank()) {
				throw new IllegalStateException(
					"AI 혜택 코치 시스템 프롬프트가 비어 있습니다."
				);
			}

			return prompt;
		} catch (IOException e) {
			throw new IllegalStateException(
				"AI 혜택 코치 시스템 프롬프트를 읽지 못했습니다.",
				e
			);
		}
	}

	public OpenAiCoachingText generateCoachingText(
		List<CalculatedCoachingData> calculatedData
	) {
		validateApiKey();

		try {
			String inputJson =
				objectMapper.writeValueAsString(
					createPromptInput(calculatedData)
				);

			Map<String, Object> requestBody =
				createRequestBody(inputJson);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(apiKey);

			HttpEntity<Map<String, Object>> request =
				new HttpEntity<>(requestBody, headers);

			ResponseEntity<String> response =
				restTemplate.postForEntity(
					apiUrl,
					request,
					String.class
				);

			return parseResponse(response.getBody());
		} catch (ResourceAccessException e) {
			throw new IllegalStateException(
				"OpenAI API 연결 또는 응답 시간이 초과되었습니다.",
				e
			);
		} catch (HttpStatusCodeException e) {
			throw new IllegalStateException(
				"OpenAI API 호출에 실패했습니다. status="
					+ e.getStatusCode().value(),
				e
			);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(
				"OpenAI 요청 또는 응답 JSON 처리에 실패했습니다.",
				e
			);
		}
	}

	private void validateApiKey() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(
				"OPENAI_API_KEY 환경변수가 설정되지 않았습니다."
			);
		}
	}

	private List<Map<String, Object>> createPromptInput(
		List<CalculatedCoachingData> calculatedData
	) {
		List<Map<String, Object>> inputs =
			new ArrayList<>();

		for (int index = 0;
		     index < calculatedData.size();
		     index++) {

			CalculatedCoachingData data =
				calculatedData.get(index);

			Map<String, Object> input =
				new LinkedHashMap<>();

			input.put(FIELD_INDEX, index);
			input.put("categoryCode", data.getCategoryCode());
			input.put("categoryName", data.getCategoryName());
			input.put(
				"usualDayOfWeek",
				data.getUsualDayOfWeek()
			);
			input.put("averageAmount", data.getAverageAmount());
			input.put("paymentCount", data.getPaymentCount());
			input.put(
				"recommendedCardName",
				data.getRecommendedCardName()
			);
			input.put(
				"expectedSavingAmount",
				data.getExpectedSavingAmount()
			);
			input.put(
				"previousMonthSpendingAmount",
				data.getPreviousMonthSpendingAmount()
			);
			input.put("strategyType", data.getStrategyType());
			input.put("nextRecommendedCardName",
				data.getNextRecommendedCardName());
			input.put("remainingUsageCount",
				data.getRemainingUsageCount());
			input.put("expectedAdditionalSavingAmount",
				data.getExpectedAdditionalSavingAmount());
			input.put(
				"appliedBenefitCondition",
				data.getAppliedBenefitCondition()
			);
			input.put("reason", data.getReason());

			inputs.add(input);
		}

		return inputs;
	}

	private Map<String, Object> createRequestBody(
		String inputJson
	) {
		Map<String, Object> requestBody =
			new LinkedHashMap<>();

		requestBody.put("model", model);
		requestBody.put("store", false);
		requestBody.put("max_output_tokens", 1000);
		requestBody.put(
			"input",
			List.of(
				Map.of(
					"role",
					"system",
					FIELD_CONTENT,
					systemPrompt
				),
				Map.of(
					"role",
					"user",
					FIELD_CONTENT,
					inputJson
				)
			)
		);
		requestBody.put(
			"text",
			Map.of(
				"format",
				createResponseFormat()
			)
		);

		return requestBody;
	}

	private Map<String, Object> createResponseFormat() {
		Map<String, Object> itemProperties =
			new LinkedHashMap<>();

		itemProperties.put(
			FIELD_INDEX,
			Map.of("type", "integer")
		);
		itemProperties.put(
			FIELD_TITLE,
			Map.of("type", JSON_TYPE_STRING)
		);
		itemProperties.put(
			FIELD_MESSAGE,
			Map.of("type", JSON_TYPE_STRING)
		);

		Map<String, Object> itemSchema =
			new LinkedHashMap<>();

		itemSchema.put("type", "object");
		itemSchema.put("properties", itemProperties);
		itemSchema.put(
			"required",
			List.of(FIELD_INDEX, FIELD_TITLE, FIELD_MESSAGE)
		);
		itemSchema.put("additionalProperties", false);

		Map<String, Object> rootProperties =
			new LinkedHashMap<>();

		rootProperties.put(
			FIELD_SUMMARY,
			Map.of("type", JSON_TYPE_STRING)
		);
		rootProperties.put(
			FIELD_ITEMS,
			Map.of(
				"type",
				"array",
				FIELD_ITEMS,
				itemSchema
			)
		);

		Map<String, Object> schema =
			new LinkedHashMap<>();

		schema.put("type", "object");
		schema.put("properties", rootProperties);
		schema.put(
			"required",
			List.of(FIELD_SUMMARY, FIELD_ITEMS)
		);
		schema.put("additionalProperties", false);

		Map<String, Object> format =
			new LinkedHashMap<>();

		format.put("type", "json_schema");
		format.put("name", "benefit_coaching");
		format.put("strict", true);
		format.put("schema", schema);

		return format;
	}

	private OpenAiCoachingText parseResponse(
		String responseBody
	) throws JsonProcessingException {
		if (responseBody == null || responseBody.isBlank()) {
			throw new IllegalStateException(
				"OpenAI API 응답이 비어 있습니다."
			);
		}

		JsonNode response =
			objectMapper.readTree(responseBody);

		if ("incomplete".equals(response.path("status").asText())) {
			throw new IllegalStateException(
				"OpenAI API 응답 생성이 완료되지 않았습니다."
			);
		}

		String outputText = findOutputText(response);

		JsonNode coachingJson =
			objectMapper.readTree(outputText);

		String summary =
			coachingJson.path(FIELD_SUMMARY).asText();

		List<OpenAiCoachingItemText> items =
			new ArrayList<>();

		for (JsonNode item : coachingJson.path(FIELD_ITEMS)) {
			items.add(
				new OpenAiCoachingItemText(
					item.path(FIELD_INDEX).asInt(),
					item.path(FIELD_TITLE).asText(),
					item.path(FIELD_MESSAGE).asText()
				)
			);
		}

		return new OpenAiCoachingText(summary, items);
	}

	private String findOutputText(JsonNode response) {
		for (JsonNode output : response.path("output")) {
			for (JsonNode content : output.path(FIELD_CONTENT)) {
				String type =
					content.path("type").asText();

				if ("refusal".equals(type)) {
					throw new IllegalStateException(
						"OpenAI가 코칭 생성을 거부했습니다."
					);
				}

				if ("output_text".equals(type)) {
					return content.path("text").asText();
				}
			}
		}

		throw new IllegalStateException(
			"OpenAI 응답에서 코칭 결과를 찾지 못했습니다."
		);
	}

	public record OpenAiCoachingText(
		String summary,
		List<OpenAiCoachingItemText> items
	) {
	}

	public record OpenAiCoachingItemText(
		int index,
		String title,
		String message
	) {
	}
}
