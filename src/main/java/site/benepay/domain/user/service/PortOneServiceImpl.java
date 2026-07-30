package site.benepay.domain.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.benepay.common.crypto.Encryptor;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.PortOneVerificationException;
import site.benepay.common.util.Sha256Util;
import site.benepay.domain.user.dto.PortOneVerifyRequestDto;
import site.benepay.domain.user.dto.PortOneVerifyResponseDto;
import site.benepay.domain.user.mapper.UserMapper;

@Service
public class PortOneServiceImpl implements PortOneService {

    private final UserMapper userMapper;
    private final Encryptor encryptor;
    private final SignupVerificationStore signupVerificationStore;
    private final String diHashSalt;

    public PortOneServiceImpl(UserMapper userMapper, Encryptor encryptor,
                               SignupVerificationStore signupVerificationStore,
                               @Value("${security.di-hash-salt}") String diHashSalt) {
        this.userMapper = userMapper;
        this.encryptor = encryptor;
        this.signupVerificationStore = signupVerificationStore;
        this.diHashSalt = diHashSalt;
    }

    @Override
    public PortOneVerifyResponseDto verify(PortOneVerifyRequestDto request) {
        String impUid = request.getImpUid();
        if (impUid == null || impUid.isBlank()) {
            throw new PortOneVerificationException("impUid is required");
        }

        // PortOne 실계정(imp_key/imp_secret)이 아직 없어 서버-to-서버 조회를 할 수 없다.
        // 실제 연동에서는 name/phone/birthday도 PortOne 응답으로 내려오지만(불러온 값이지
        // 사용자가 직접 입력한 값이 아니다), 지금은 그 화면을 대신하는 프론트 모달에서 받은
        // 값을 그대로 믿는다. CI/DI만 impUid를 시드로 결정적으로 만든다 - 같은 impUid로
        // 두 번 호출하면 같은 CI/DI가 나와서, 아래 중복 가입 체크가 실제 연동 때와 동일한
        // 모양으로 동작한다. 실제 키가 생기면 이 메서드 본문을 PortOne REST 호출로 바꾸고
        // name/phone/birthday도 그 응답에서 채우면 된다.
        String uniqueKey = "mock-ci-" + impUid;
        String uniqueInSite = "mock-di-" + impUid;

        String ciEncrypted = encryptor.encrypt(uniqueKey);
        String diHash = Sha256Util.hashWithSalt(uniqueInSite, diHashSalt);

        // 회원가입까지 안 가고 인증 시점에 먼저 걸러야 "인증은 됐는데 이미 가입된 사람"이라고
        // 바로 알려줄 수 있다. 다만 이게 최종 방어선은 아니다 - 진짜 유일성은 users.di UNIQUE가
        // 지키고, signUp()에서 한 번 더 확인한다(인증 이후 다른 요청이 먼저 가입했을 수 있음).
        if (userMapper.existsByDiHash(diHash)) {
            throw new DuplicateUserException("identity already registered");
        }

        SignupVerificationStore.VerifiedIdentity identity = new SignupVerificationStore.VerifiedIdentity(
                request.getName(), request.getPhoneNumber(), request.getBirthDate(), ciEncrypted, diHash);
        String token = signupVerificationStore.issue(identity);

        return new PortOneVerifyResponseDto(token);
    }
}
