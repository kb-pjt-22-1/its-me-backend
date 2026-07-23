package site.benepay.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256UtilTest {

    @Test
    void sameInputAndSaltProduceTheSameHash() {
        String hash1 = Sha256Util.hashWithSalt("some-di-value", "pepper");
        String hash2 = Sha256Util.hashWithSalt("some-di-value", "pepper");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void differentSaltProducesDifferentHash() {
        String hash1 = Sha256Util.hashWithSalt("some-di-value", "pepper-a");
        String hash2 = Sha256Util.hashWithSalt("some-di-value", "pepper-b");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void differentInputProducesDifferentHash() {
        String hash1 = Sha256Util.hashWithSalt("value-a", "pepper");
        String hash2 = Sha256Util.hashWithSalt("value-b", "pepper");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void outputIsA64CharacterLowercaseHexString() {
        String hash = Sha256Util.hashWithSalt("some-di-value", "pepper");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }
}
