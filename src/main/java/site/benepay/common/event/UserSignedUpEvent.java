package site.benepay.common.event;

/**
 * 카드 자동 연동을 위한 회원가입 이벤트 발행용 레코드
 * <p>
 *     데이터는 불변이며, 회원가입 단에서 유효성 검증이 끝났으므로
 *     카드 도메인에서 따로 유효성 검증을 할 필요는 없습니다.
 * </p>
 * @param userId
 * @param ciHash
 */
public record UserSignedUpEvent(Long userId, String ciHash) {
}
