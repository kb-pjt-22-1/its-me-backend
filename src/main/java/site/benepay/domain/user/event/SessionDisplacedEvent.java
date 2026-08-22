package site.benepay.domain.user.event;

/**
 * 같은 계정으로 새 로그인이 발생해 기존 기기의 세션(access 토큰)이 강제로 무효화됐을 때
 * 발행된다. TokenServiceImpl.issueTokenPair가 새 토큰을 발급하기 직전에 이미 저장돼 있던
 * 세션(refresh 상태)을 발견한 경우에만 발행하므로, 첫 로그인에는 발행되지 않는다.
 */
public record SessionDisplacedEvent(Long userId) {
}
