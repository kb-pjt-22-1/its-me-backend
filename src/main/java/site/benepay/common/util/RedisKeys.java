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
}
