package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.SignupIdentityConfirmRequestDto;
import site.benepay.domain.user.dto.SignupIdentityConfirmResponseDto;
import site.benepay.domain.user.dto.SignupIdentityRequestDto;
import site.benepay.domain.user.dto.SignupIdentityRequestResponseDto;

/**
 * 회원가입 1단계(휴대폰 본인인증). 예전 PortOneServiceImpl(완전 mock, 프론트가 보내는
 * 이름/전화번호/생년월일을 검증 없이 신뢰)을 대체한다 - 이제는 ① 내부 중복 가입 여부,
 * ② KB Mock Server 실명 회원 여부를 서버가 직접 확인한 뒤에만 인증번호를 보낸다.
 */
public interface SignupIdentityService {

	SignupIdentityRequestResponseDto requestVerification(SignupIdentityRequestDto request);

	SignupIdentityConfirmResponseDto confirmVerification(SignupIdentityConfirmRequestDto request);
}
