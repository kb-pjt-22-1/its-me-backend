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

    @Test
    void hashWithoutSaltIsDeterministic() {
        String hash1 = Sha256Util.hash("홍길동");
        String hash2 = Sha256Util.hash("홍길동");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
        assertThat(hash1).matches("^[0-9a-f]{64}$");
    }

    @Test
    void hashWithoutSaltProducesDifferentHashesForDifferentInput() {
        String hash1 = Sha256Util.hash("홍길동");
        String hash2 = Sha256Util.hash("김철수");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hashWithoutSaltDiffersFromHashWithSaltForTheSameValue() {
        String withoutSalt = Sha256Util.hash("some-name");
        String withSalt = Sha256Util.hashWithSalt("some-name", "pepper");

        assertThat(withoutSalt).isNotEqualTo(withSalt);
    }
}
