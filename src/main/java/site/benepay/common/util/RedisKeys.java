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


}
