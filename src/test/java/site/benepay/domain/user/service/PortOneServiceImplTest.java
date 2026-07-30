package site.benepay.domain.user.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(signupVerificationStore.issue(any())).thenReturn("issued-token");

        portOneService.verify(request("imp-uid-1"));

        ArgumentCaptor<SignupVerificationStore.VerifiedIdentity> captor =
                ArgumentCaptor.forClass(SignupVerificationStore.VerifiedIdentity.class);
        verify(signupVerificationStore).issue(captor.capture());

        SignupVerificationStore.VerifiedIdentity identity = captor.getValue();
        assertThat(identity.ciEncrypted).isEqualTo("encrypted-ci");
        // encryptor.encrypt()에 넘어간 원문(mock-ci-imp-uid-1)이 그대로 저장되면 안 된다 -
        // 암호화되지 않은 CI가 남는 것과 같기 때문이다.
        assertThat(identity.ciEncrypted).isNotEqualTo("mock-ci-imp-uid-1");
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
