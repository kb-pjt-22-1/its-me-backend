package site.benepay.domain.benefit.service;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.domain.benefit.dto.BenefitCoachDataDto.CalculatedCoachingData;

class OpenAiClientTest {

	private static final String API_KEY = "test-api-key";
	private static final String API_URL =
		"https://api.openai.test/v1/responses";
	private static final String MODEL = "gpt-test";

	@Test
	void generateCoachingTextParsesCompletedResponse() {
		OpenAiClient client = createClient(API_KEY);
		MockRestServiceServer server = bindServer(client);

		server.expect(requestTo(API_URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(
				header(
					HttpHeaders.AUTHORIZATION,
					"Bearer " + API_KEY
				)
			)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().string(containsString(MODEL)))
			.andExpect(content().string(containsString("json_schema")))
			.andExpect(content().string(containsString("주유소")))
			.andRespond(
				withSuccess(
					completedResponse(),
					MediaType.APPLICATION_JSON
				)
			);

		OpenAiClient.OpenAiCoachingText result =
			client.generateCoachingText(
				List.of(calculatedData())
			);

		assertThat(result.summary())
			.isEqualTo("최근 소비 패턴에 따른 카드 추천입니다.");
		assertThat(result.items()).hasSize(1);

		OpenAiClient.OpenAiCoachingItemText item =
			result.items().get(0);

		assertThat(item.index()).isZero();
		assertThat(item.title())
			.isEqualTo("토요일 주유소에는");
		assertThat(item.message())
			.contains("215원의 추가 혜택");

		server.verify();
	}

	@Test
	void generateCoachingTextRejectsMissingApiKey() {
		OpenAiClient client = createClient(" ");

		assertThatThrownBy(
			() -> client.generateCoachingText(List.of())
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
				"OPENAI_API_KEY 환경변수가 설정되지 않았습니다."
			);
	}

	@ParameterizedTest
	@MethodSource("invalidSuccessfulResponses")
	void generateCoachingTextRejectsInvalidSuccessfulResponse(
		String responseBody,
		String expectedMessage
	) {
		OpenAiClient client = createClient(API_KEY);
		MockRestServiceServer server = bindServer(client);

		server.expect(requestTo(API_URL))
			.andRespond(
				withSuccess(
					responseBody,
					MediaType.APPLICATION_JSON
				)
			);

		assertThatThrownBy(
			() -> client.generateCoachingText(List.of())
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(expectedMessage);
	}

	@Test
	void generateCoachingTextRejectsRefusal() {
		OpenAiClient client = createClient(API_KEY);
		MockRestServiceServer server = bindServer(client);

		server.expect(requestTo(API_URL))
			.andRespond(
				withSuccess(
					"""
						{
						  "status": "completed",
						  "output": [
						    {"content": [{"type": "refusal"}]}
						  ]
						}
						""",
					MediaType.APPLICATION_JSON
				)
			);

		assertThatThrownBy(
			() -> client.generateCoachingText(List.of())
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("OpenAI가 코칭 생성을 거부했습니다.");
	}

	@Test
	void generateCoachingTextWrapsInvalidJson() {
		OpenAiClient client = createClient(API_KEY);
		MockRestServiceServer server = bindServer(client);

		server.expect(requestTo(API_URL))
			.andRespond(
				withSuccess("not-json", MediaType.APPLICATION_JSON)
			);

		assertThatThrownBy(
			() -> client.generateCoachingText(List.of())
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
				"OpenAI 요청 또는 응답 JSON 처리에 실패했습니다."
			)
			.hasCauseInstanceOf(JsonProcessingException.class);
	}

	@Test
	void generateCoachingTextWrapsHttpError() {
		OpenAiClient client = createClient(API_KEY);
		MockRestServiceServer server = bindServer(client);

		server.expect(requestTo(API_URL))
			.andRespond(
				withStatus(HttpStatus.BAD_GATEWAY)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"error\":\"upstream error\"}")
			);

		assertThatThrownBy(
			() -> client.generateCoachingText(List.of())
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
				"OpenAI API 호출에 실패했습니다. status=502"
			);
	}

	@Test
	void generateCoachingTextWrapsTimeout() {
		OpenAiClient client = createClient(API_KEY);
		MockRestServiceServer server = bindServer(client);

		server.expect(requestTo(API_URL))
			.andRespond(
				withException(
					new SocketTimeoutException("read timed out")
				)
			);

		assertThatThrownBy(
			() -> client.generateCoachingText(List.of())
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
				"OpenAI API 연결 또는 응답 시간이 초과되었습니다."
			);
	}

	@Test
	void constructorRejectsBlankSystemPrompt() {
		Resource blankPrompt =
			new ByteArrayResource(
				"   ".getBytes(StandardCharsets.UTF_8)
			);

		assertThatThrownBy(
			() -> createClient(API_KEY, blankPrompt)
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
				"AI 혜택 코치 시스템 프롬프트가 비어 있습니다."
			);
	}

	@Test
	void constructorWrapsPromptReadFailure() throws IOException {
		Resource resource = mock(Resource.class);
		when(resource.getInputStream())
			.thenThrow(new IOException("cannot read"));

		assertThatThrownBy(
			() -> createClient(API_KEY, resource)
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
				"AI 혜택 코치 시스템 프롬프트를 읽지 못했습니다."
			)
			.hasCauseInstanceOf(IOException.class);
	}

	private OpenAiClient createClient(String apiKey) {
		Resource prompt =
			new ByteArrayResource(
				"계산값을 변경하지 마세요."
					.getBytes(StandardCharsets.UTF_8)
			);

		return createClient(apiKey, prompt);
	}

	private OpenAiClient createClient(
		String apiKey,
		Resource prompt
	) {
		return new OpenAiClient(
			new ObjectMapper(),
			apiKey,
			API_URL,
			MODEL,
			1_000,
			1_000,
			prompt
		);
	}

	private MockRestServiceServer bindServer(
		OpenAiClient client
	) {
		RestTemplate restTemplate =
			(RestTemplate)ReflectionTestUtils.getField(
				client,
				"restTemplate"
			);

		assertThat(restTemplate).isNotNull();

		return MockRestServiceServer
			.bindTo(restTemplate)
			.build();
	}

	private static Stream<Arguments> invalidSuccessfulResponses() {
		return Stream.of(
			Arguments.of(
				"",
				"OpenAI API 응답이 비어 있습니다."
			),
			Arguments.of(
				"{\"status\":\"incomplete\"}",
				"OpenAI API 응답 생성이 완료되지 않았습니다."
			),
			Arguments.of(
				"{\"status\":\"completed\",\"output\":[]}",
				"OpenAI 응답에서 코칭 결과를 찾지 못했습니다."
			)
		);
	}

	private CalculatedCoachingData calculatedData() {
		CalculatedCoachingData data =
			mock(CalculatedCoachingData.class);

		when(data.getCategoryCode()).thenReturn("5541");
		when(data.getCategoryName()).thenReturn("주유소");
		when(data.getUsualDayOfWeek()).thenReturn("토요일");
		when(data.getPaymentCount()).thenReturn(3);
		when(data.getRecommendedCardName())
			.thenReturn("NEED Global 카드");
		when(data.getExpectedSavingAmount()).thenReturn(215L);
		when(data.getPreviousMonthSpendingAmount())
			.thenReturn(100_000L);
		when(data.getStrategyType()).thenReturn("BENEFIT_GUIDE");
		when(data.getExpectedAdditionalSavingAmount())
			.thenReturn(215L);
		when(data.getAppliedBenefitCondition())
			.thenReturn("주유 할인");
		when(data.getReason())
			.thenReturn("215원의 혜택이 예상됩니다.");

		return data;
	}

	private String completedResponse() {
		return """
			{
			  "status": "completed",
			  "output": [
			    {
			      "content": [
			        {
			          "type": "output_text",
			          "text": "{\\\"summary\\\":\\\"최근 소비 패턴에 따른 카드 추천입니다.\\\",\\\"items\\\":[{\\\"index\\\":0,\\\"title\\\":\\\"토요일 주유소에는\\\",\\\"message\\\":\\\"NEED Global 카드를 사용하면 215원의 추가 혜택을 기대할 수 있어요.\\\"}]}"
			        }
			      ]
			    }
			  ]
			}
			""";
	}
}
