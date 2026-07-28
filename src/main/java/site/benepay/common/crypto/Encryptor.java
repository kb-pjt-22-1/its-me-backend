package site.benepay.common.crypto;

/**
 * 복호화가 가능해야 하는 값에 쓴다. 비밀번호·PIN처럼 원본을 되찾을 필요가 없는 값은
 * 여전히 PasswordEncoder(단방향 해시) 쪽이 맞다.
 */
public interface Encryptor {

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
