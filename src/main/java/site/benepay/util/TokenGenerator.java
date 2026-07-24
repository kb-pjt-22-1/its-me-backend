package site.benepay.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    public static String generatePaymentToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "BP_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateApprovalCode() {
        int number = 100000 + RANDOM.nextInt(900000);
        return "BP" + System.currentTimeMillis() + number;
    }
}
