package site.benepay.common.exception;

/**
 * 개발용 자동 로그인이 꺼진 상태에서 호출됐을 때. 403이 아니라 404로 매핑한다.
 * 403은 "여기 그런 기능이 있긴 하다"를 알려주는 셈이라, 꺼져 있을 때는 존재 자체를 감춘다.
 */
public class DevLoginDisabledException extends RuntimeException {

    public DevLoginDisabledException(String message) {
        super(message);
    }
}
