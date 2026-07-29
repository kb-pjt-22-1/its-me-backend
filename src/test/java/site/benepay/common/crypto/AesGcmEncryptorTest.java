package site.benepay.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.benepay.common.crypto.mapper.EncryptionKeyMapper;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AesGcmEncryptorTest {

    // PortOne unique_key와 비슷한 길이(88자)로 잡아 ci_encrypted VARCHAR(200)에 들어가는지 본다.
    private static final String CI_SAMPLE =
            "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567";

    @Mock
    private EncryptionKeyMapper encryptionKeyMapper;

    private AesGcmEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AesGcmEncryptor(encryptionKeyMapper);
    }

    private void givenKeyOfBytes(int byteLength) {
        EncryptionKey key = new EncryptionKey();
        key.setKeyAlias("CI");
        key.setKeyValue(Base64.getEncoder().encodeToString(new byte[byteLength]));
        key.setAlgorithm("AES256");
        key.setActive(true);
        when(encryptionKeyMapper.findActiveByAlias("CI")).thenReturn(Optional.of(key));
    }

    @Test
    void decryptRecoversTheOriginalValue() {
        givenKeyOfBytes(32);

        String encrypted = encryptor.encrypt(CI_SAMPLE);

        assertThat(encrypted).isNotEqualTo(CI_SAMPLE);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(CI_SAMPLE);
    }

    @Test
    void sameInputProducesDifferentCipherTextSoEqualIdentitiesAreNotLinkable() {
        givenKeyOfBytes(32);

        assertThat(encryptor.encrypt(CI_SAMPLE)).isNotEqualTo(encryptor.encrypt(CI_SAMPLE));
    }

    @Test
    void cipherTextFitsTheCiEncryptedColumn() {
        givenKeyOfBytes(32);

        assertThat(encryptor.encrypt(CI_SAMPLE).length()).isLessThanOrEqualTo(200);
    }

    @Test
    void tamperedCipherTextIsRejectedByTheAuthenticationTag() {
        givenKeyOfBytes(32);
        String encrypted = encryptor.encrypt(CI_SAMPLE);

        byte[] payload = Base64.getDecoder().decode(encrypted);
        payload[payload.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(payload);

        assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void keyIsLoadedOnceAndReused() {
        givenKeyOfBytes(32);

        encryptor.encrypt(CI_SAMPLE);
        encryptor.encrypt(CI_SAMPLE);

        verify(encryptionKeyMapper, times(1)).findActiveByAlias("CI");
    }

    @Test
    void shortKeyIsRejectedInsteadOfSilentlyWeakeningToAes128() {
        givenKeyOfBytes(16);

        assertThatThrownBy(() -> encryptor.encrypt(CI_SAMPLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32-byte key");
    }

    @Test
    void missingKeyFailsLoudly() {
        when(encryptionKeyMapper.findActiveByAlias("CI")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encryptor.encrypt(CI_SAMPLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no active encryption key");
    }
}
