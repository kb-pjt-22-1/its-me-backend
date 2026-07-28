package site.benepay.common.crypto;

import org.springframework.stereotype.Component;
import site.benepay.common.crypto.mapper.EncryptionKeyMapper;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256으로 양방향 암호화한다. 키는 encryption_keys 테이블에서 읽는다.
 *
 * <p>암호문과 키가 같은 DB에 있으므로, DB 전체가 털리는 상황은 이 암호화로 막지 못한다.
 * 부분 덤프나 테이블 단위 유출까지만 방어된다. 임시 구성이며, 실제로 지켜야 할 단계에서는
 * 키를 KMS나 별도 시크릿 저장소로 옮겨야 한다.
 */
@Component
public class AesGcmEncryptor implements Encryptor {

    // 지금 암호화 대상은 CI 하나뿐이다. 다른 용도가 생기면 alias별로 빈을 나눠야 한다.
    private static final String KEY_ALIAS = "CI";

    // CBC가 아니라 GCM인 이유: 인증 태그가 붙어 복호화 시 위변조를 잡아낸다. DB를 직접 고칠 수
    // 있는 공격자가 암호문 비트를 뒤집어도 조용히 통과하는 상황을 막는다.
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final EncryptionKeyMapper encryptionKeyMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    // 키는 런타임에 바뀌지 않는데 가입할 때마다 조회하면 낭비다. 다만 기동 시점에 읽으면
    // DB가 아직 안 떠 있을 때 컨텍스트가 통째로 실패하므로 첫 사용 때 채운다.
    private volatile SecretKey cachedKey;

    public AesGcmEncryptor(EncryptionKeyMapper encryptionKeyMapper) {
        this.encryptionKeyMapper = encryptionKeyMapper;
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        // 키 조회는 try 밖에 둔다. 안에서 부르면 "키가 없다", "길이가 틀렸다" 같은 설정 오류가
        // 아래 catch에 걸려 "failed to encrypt value"로 덮이고, 원인을 알 수 없게 된다.
        SecretKey key = key();

        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV는 비밀이 아니지만 복호화에 반드시 필요하다. 컬럼을 하나 더 두는 대신 앞에 붙여
            // 한 값으로 다닌다.
            byte[] payload = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherBytes, 0, payload, iv.length, cipherBytes.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("failed to encrypt value", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        SecretKey key = key();

        byte[] payload = Base64.getDecoder().decode(cipherText);
        if (payload.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("cipher text is too short to contain an IV");
        }

        try {
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            byte[] cipherBytes = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("failed to decrypt value", e);
        }
    }

    private SecretKey key() {
        SecretKey key = cachedKey;
        if (key == null) {
            synchronized (this) {
                key = cachedKey;
                if (key == null) {
                    key = loadKey();
                    cachedKey = key;
                }
            }
        }
        return key;
    }

    private SecretKey loadKey() {
        EncryptionKey encryptionKey = encryptionKeyMapper.findActiveByAlias(KEY_ALIAS)
                .orElseThrow(() -> new IllegalStateException(
                        "no active encryption key registered for alias " + KEY_ALIAS));

        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey.getKeyValue());
        if (keyBytes.length != 32) {
            // 짧은 키가 들어오면 AES-128/192로 조용히 내려앉는다. 의도한 강도가 아니면 차라리 멈춘다.
            throw new IllegalStateException(
                    "AES-256 requires a 32-byte key but alias " + KEY_ALIAS + " holds " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
