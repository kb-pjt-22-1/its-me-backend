package site.benepay.common.util;

public final class RedisKeys {

	private RedisKeys() {
	}

	public static String loginFailure(Long userId) {
		return "login:failure:" + userId;
	}

	public static String loginLock(Long userId) {
		return "login:lock:" + userId;
	}

	public static String refresh(Long userId) {
		return "refresh:" + userId;
	}

	public static String pinFailure(Long userId) {
		return "pin:failure:" + userId;
	}

	public static String pinLock(Long userId) {
		return "pin:lock:" + userId;
	}

	public static String passwordFailure(Long userId) {
		return "password:failure:" + userId;
	}

	public static String passwordLock(Long userId) {
		return "password:lock:" + userId;
	}

	public static String blacklist(String jti) {
		return "blacklist:" + jti;
	}

	public static String alert(Long userId) {
		return "alert:" + userId;
	}

	public static String signupVerification(String token) {
		return "signup:verify:" + token;
	}

	// 회원가입 1단계(휴대폰 본인인증) 전용 - 아직 userId가 없으므로 전화번호를 키로 잠근다.

	public static String signupIdentityFailure(String phoneNumber) {
		return "signup:identity:failure:" + phoneNumber;
	}

	public static String signupIdentityLock(String phoneNumber) {
		return "signup:identity:lock:" + phoneNumber;
	}

	public static String signupOtp(String phoneNumber) {
		return "signup:otp:" + phoneNumber;
	}

	public static String signupOtpFailure(String phoneNumber) {
		return "signup:otp:failure:" + phoneNumber;
	}

	public static String signupOtpLock(String phoneNumber) {
		return "signup:otp:lock:" + phoneNumber;
	}

	public static String paymentToken(String paymentTokenId) {
		return "payment:token:" + paymentTokenId;
	}

	// 매장 위치 GEO 인덱스(MerchantGeoSyncScheduler가 매일 새벽 MySQL에서 재적재) - 카테고리
	// 필터가 있는 조회도 지원해야 해서 전체용 키 하나와 카테고리별 키를 따로 둔다. GEOSEARCH는
	// 좌표 기반 필터만 지원하고 임의 속성(카테고리) 필터가 안 되기 때문.
	// 파라미터 없이 항상 같은 값만 반환하는 메서드는 SonarQube(S3400)가 상수로 선언하라고
	// 지적해서, 이 둘만 다른 키들과 다르게 메서드가 아니라 상수다.
	public static final String MERCHANT_GEO_ALL = "merchants:geo:all";

	// 여러 인스턴스가 동시에 뜰 걸 대비한 배치 중복 실행 방지 락.
	public static final String MERCHANT_GEO_SYNC_LOCK = "merchants:geo:sync:lock";

	public static String merchantGeoCategory(String categoryCode) {
		return "merchants:geo:" + categoryCode;
	}

	// AI 혜택 코치 결과 캐시. 계산 자체(OpenAI 호출)가 유저당 실비용이라, 매장 GEO 인덱스처럼
	// 전체를 미리 채워두는 배치 대신 지연 계산으로 간다 - 그 주에 화면을 안 여는 유저에게는
	// 아예 계산이 안 일어난다(BenefitServiceImpl.getBenefitCoaching 참고).
	public static String benefitCoach(Long userId) {
		return "benefit:coach:" + userId;
	}

}
