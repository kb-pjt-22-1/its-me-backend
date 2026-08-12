package site.benepay.domain.card.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.common.event.UserSignedUpEvent;
import site.benepay.domain.card.service.CardSyncService;

/**
 * 회원가입 트랜잭션이 정상 COMMIT된 뒤 기존 KB 보유 카드를 동기화한다.
 *
 * 회원가입과 카드 연동을 서로 다른 실패 단위로 분리하는 역할이다.
 * 카드사 장애가 발생해도 이미 완료된 회원가입은 취소하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSignedUpCardSyncHandler {

	private final CardSyncService cardSyncService;

	/**
	 * AFTER_COMMIT을 사용하는 이유:
	 * users INSERT가 확정된 뒤 user_cards가 해당 user_id를 참조하게 하기 위함이다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(UserSignedUpEvent event) {
		try {
			int insertedCount = cardSyncService.syncCards(
				event.userId(),
				event.ciHash()
			);

			log.info(
				"KB 카드 자동 연동 완료. userId={}, insertedCount={}",
				event.userId(),
				insertedCount
			);
		} catch (Exception e) {
			/*
			 * 회원가입은 이미 COMMIT됐으므로 예외를 밖으로 전파해
			 * 회원가입을 실패한 것처럼 처리하지 않는다.
			 */
			log.error(
				"KB 카드 자동 연동 실패. userId={}",
				event.userId(),
				e
			);
		}
	}
}
