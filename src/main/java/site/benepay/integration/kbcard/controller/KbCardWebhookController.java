package site.benepay.integration.kbcard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.integration.kbcard.dto.CardIssuedWebhookRequestDto;
import site.benepay.integration.kbcard.service.KbCardWebhookService;

/**
 * KB카드 Mock Server가 전달하는 카드 발급 Webhook을 수신한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks/kb-card")
public class KbCardWebhookController {

	private final KbCardWebhookService kbCardWebhookService;

	@PostMapping("/card-issued")
	public ResponseEntity<Void> cardIssued(
		@RequestBody CardIssuedWebhookRequestDto request
	) {

		kbCardWebhookService.processCardIssued(request);

		return ResponseEntity.ok().build();
	}
}
