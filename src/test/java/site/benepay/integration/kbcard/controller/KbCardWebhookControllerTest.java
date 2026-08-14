package site.benepay.integration.kbcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import site.benepay.integration.kbcard.dto.CardIssuedWebhookRequestDto;
import site.benepay.integration.kbcard.service.KbCardWebhookService;

@ExtendWith(MockitoExtension.class)
class KbCardWebhookControllerTest {

	@Mock
	private KbCardWebhookService webhookService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new KbCardWebhookController(webhookService))
			.build();
	}

	@Test
	@DisplayName("카드 발급 Webhook JSON을 DTO에 바인딩해 서비스에 전달하고 HTTP 200을 반환한다")
	void cardIssuedBindsJsonAndCallsService() throws Exception {
		String json = """
			{
			  "eventId": "event-001",
			  "ciHash": "ci-hash-001",
			  "cardReferenceId": "card-ref-001",
			  "issuerProductCode": "product-001",
			  "cardLast4": "1234",
			  "cardType": "CREDIT",
			  "cardStatus": "ACTIVE"
			}
			""";

		mockMvc.perform(post("/api/v1/webhooks/kb-card/card-issued")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		ArgumentCaptor<CardIssuedWebhookRequestDto> captor =
			ArgumentCaptor.forClass(CardIssuedWebhookRequestDto.class);
		verify(webhookService, times(1)).processCardIssued(captor.capture());
		CardIssuedWebhookRequestDto request = captor.getValue();
		assertThat(request.getEventId()).isEqualTo("event-001");
		assertThat(request.getCiHash()).isEqualTo("ci-hash-001");
		assertThat(request.getCardReferenceId()).isEqualTo("card-ref-001");
		assertThat(request.getIssuerProductCode()).isEqualTo("product-001");
		assertThat(request.getCardLast4()).isEqualTo("1234");
		assertThat(request.getCardType()).isEqualTo("CREDIT");
		assertThat(request.getCardStatus()).isEqualTo("ACTIVE");
	}
}
