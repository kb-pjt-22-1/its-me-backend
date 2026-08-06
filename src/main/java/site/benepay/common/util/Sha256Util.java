package site.benepay.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256Util {

	private Sha256Util() {
	}

	public static String hashWithSalt(String value, String salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(salt.getBytes(StandardCharsets.UTF_8));
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return toHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

	/**
	 * 솔트 없는 SHA-256. CI 해시(users.ci_hash)에 쓴다 - 카드 서비스가 같은 값
	 * (SHA-256(UTF-8(name)))으로 ci_hash를 매칭 키로 쓰므로, 이쪽에 솔트를 넣으면 그 매칭이 깨진다.
	 */
	public static String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return toHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
