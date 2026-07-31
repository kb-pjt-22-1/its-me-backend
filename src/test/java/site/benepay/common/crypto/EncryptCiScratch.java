package site.benepay.common.crypto;

import site.benepay.common.crypto.mapper.EncryptionKeyMapper;

import java.util.Optional;

/**
 * CI 평문 하나를 암호화해서 콘솔에 찍어보는 용도. IntelliJ에서 이 파일의 main()을 바로
 * Run하면 된다 (JUnit 테스트가 아니라서 ./gradlew test에는 안 걸린다).
 *
 * <p>키는 로컬 benepay.encryption_keys의 실제 활성 키(alias='CI')를 그대로 박아뒀다. 그래서
 * 여기서 나온 값을 users.ci_encrypted에 수동으로 넣으면, 지금 떠 있는 앱이 그대로 복호화할
 * 수 있다. DB에서 키를 재발급하면 이 값도 다시 뽑아야 한다.
 *
 * <p>커밋 대상이 아니다. 다 쓰면 지워도 된다.
 */
public class EncryptCiScratch {

    // benepay.encryption_keys WHERE key_alias='CI'의 key_value 그대로.
    private static final String LOCAL_DB_KEY_BASE64 = "ZHsppXZssP/iNxL0DhGMYRpfzs9ksDslBH+j1E8UeVU=";

    public static void main(String[] args) {
        String plainCi = args.length > 0 ? args[0] : "sample-ci-0000000000000000000000000000000";

        AesGcmEncryptor encryptor = new AesGcmEncryptor(fixedKeyMapper(LOCAL_DB_KEY_BASE64));

        String encrypted = encryptor.encrypt(plainCi);
        System.out.println("plain:     " + plainCi);
        System.out.println("encrypted: " + encrypted);
        System.out.println("decrypted: " + encryptor.decrypt(encrypted)); // 왕복 확인용
    }

    private static EncryptionKeyMapper fixedKeyMapper(String base64Key) {
        return keyAlias -> {
            EncryptionKey key = new EncryptionKey();
            key.setKeyAlias(keyAlias);
            key.setKeyValue(base64Key);
            key.setAlgorithm("AES256");
            key.setActive(true);
            return Optional.of(key);
        };
    }
}
