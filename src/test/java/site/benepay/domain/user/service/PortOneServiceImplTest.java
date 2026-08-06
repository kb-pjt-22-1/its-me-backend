package site.benepay.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.crypto.Encryptor;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.PortOneVerificationException;
import site.benepay.domain.user.dto.PortOneVerifyRequestDto;
import site.benepay.domain.user.dto.PortOneVerifyResponseDto;
import site.benepay.domain.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class PortOneServiceImplTest {

	private static final String DI_HASH_SALT = "unit-test-salt";

	@Mock
	private UserMapper userMapper;

	@Mock
	private Encryptor encryptor;

	@Mock
	private SignupVerificationStore signupVerificationStore;

	private PortOneService portOneService;

	@BeforeEach
	void setUp() {
		portOneService = new PortOneServiceImpl(userMapper, encryptor, signupVerificationStore, DI_HASH_SALT);
	}

	private PortOneVerifyRequestDto request(String impUid) {
		return new PortOneVerifyRequestDto(impUid, "홍길동", "010-1111-2222", "19900101");
	}

	@Test
	void verifyIssuesATokenWhenTheIdentityIsNotAlreadyRegistered() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(false);
		when(signupVerificationStore.issue(any())).thenReturn("issued-token");

		PortOneVerifyResponseDto response = portOneService.verify(request("imp-uid-1"));

		assertThat(response.getVerificationToken()).isEqualTo("issued-token");
	}

	@Test
	void verifyStoresTheEncryptedCiAndHashedDiNotTheRawValues() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(false);
		when(userMapper.existsByCiHash(anyString())).thenReturn(false);
		when(signupVerificationStore.issue(any())).thenReturn("issued-token");

		portOneService.verify(request("imp-uid-1"));

		ArgumentCaptor<SignupVerificationStore.VerifiedIdentity> captor =
			ArgumentCaptor.forClass(SignupVerificationStore.VerifiedIdentity.class);
		verify(signupVerificationStore).issue(captor.capture());

		SignupVerificationStore.VerifiedIdentity identity = captor.getValue();
		assertThat(identity.ciEncrypted).isEqualTo("encrypted-ci");
	}

	// CI 값은 이제 impUid가 아니라 이름(name)에서 나온다 - 카드 서비스도 같은 이름으로
	// SHA-256(UTF-8(name))을 ci_hash로 채워 두므로, 이 식이 흐트러지면 카드 자동 연동 매칭이
	// 조용히 깨진다(연동 로직 자체는 이 저장소 밖, 카드 서비스에 있다).
	@Test
	void ciHashIsPlainSha256OfTheName() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(false);
		when(userMapper.existsByCiHash(anyString())).thenReturn(false);
		when(signupVerificationStore.issue(any())).thenReturn("issued-token");

		portOneService.verify(request("imp-uid-1"));

		ArgumentCaptor<SignupVerificationStore.VerifiedIdentity> captor =
			ArgumentCaptor.forClass(SignupVerificationStore.VerifiedIdentity.class);
		verify(signupVerificationStore).issue(captor.capture());

		assertThat(captor.getValue().ciHash).isEqualTo(site.benepay.common.util.Sha256Util.hash("홍길동"));
	}

	@Test
	void encryptorReceivesTheNameNotTheImpUidAsTheCiPlaintext() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(false);
		when(userMapper.existsByCiHash(anyString())).thenReturn(false);
		when(signupVerificationStore.issue(any())).thenReturn("issued-token");

		portOneService.verify(request("imp-uid-1"));

		ArgumentCaptor<String> plaintext = ArgumentCaptor.forClass(String.class);
		verify(encryptor).encrypt(plaintext.capture());
		assertThat(plaintext.getValue()).isEqualTo("홍길동");
	}

	@Test
	void verifyRejectsWhenTheCiHashIsAlreadyRegistered() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(false);
		when(userMapper.existsByCiHash(anyString())).thenReturn(true);

		assertThatThrownBy(() -> portOneService.verify(request("imp-uid-1")))
			.isInstanceOf(DuplicateUserException.class);

		verify(signupVerificationStore, never()).issue(any());
	}

	@Test
	void verifyUsesTheNamePhoneAndBirthDateSuppliedByTheCaller() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(false);
		when(signupVerificationStore.issue(any())).thenReturn("issued-token");

		portOneService.verify(request("imp-uid-1"));

		ArgumentCaptor<SignupVerificationStore.VerifiedIdentity> captor =
			ArgumentCaptor.forClass(SignupVerificationStore.VerifiedIdentity.class);
		verify(signupVerificationStore).issue(captor.capture());

		SignupVerificationStore.VerifiedIdentity identity = captor.getValue();
		assertThat(identity.name).isEqualTo("홍길동");
		assertThat(identity.phoneNumber).isEqualTo("010-1111-2222");
		assertThat(identity.birthDate).isEqualTo("19900101");
	}

	@Test
	void sameImpUidProducesTheSameDiHashSoDuplicateVerificationIsDetectable() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(false);
		when(signupVerificationStore.issue(any())).thenReturn("issued-token");

		portOneService.verify(request("imp-uid-same"));
		portOneService.verify(request("imp-uid-same"));

		ArgumentCaptor<SignupVerificationStore.VerifiedIdentity> captor =
			ArgumentCaptor.forClass(SignupVerificationStore.VerifiedIdentity.class);
		verify(signupVerificationStore, times(2)).issue(captor.capture());

		assertThat(captor.getAllValues().get(0).diHash).isEqualTo(captor.getAllValues().get(1).diHash);
	}

	@Test
	void verifyRejectsAlreadyRegisteredIdentityAndNeverIssuesAToken() {
		when(encryptor.encrypt(anyString())).thenReturn("encrypted-ci");
		when(userMapper.existsByDiHash(anyString())).thenReturn(true);

		assertThatThrownBy(() -> portOneService.verify(request("imp-uid-1")))
			.isInstanceOf(DuplicateUserException.class);

		verify(signupVerificationStore, never()).issue(any());
	}

	@Test
	void verifyRejectsBlankImpUid() {
		assertThatThrownBy(() -> portOneService.verify(request(" ")))
			.isInstanceOf(PortOneVerificationException.class);

		verify(signupVerificationStore, never()).issue(any());
	}
}
