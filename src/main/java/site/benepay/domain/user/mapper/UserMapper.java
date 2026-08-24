package site.benepay.domain.user.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.user.vo.User;

public interface UserMapper {

	void insert(User user);

	Optional<User> findByUserId(Long userId);

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByCiHash(@Param("ciHash") String ciHash);

	boolean existsByLoginId(String loginId);

	boolean existsByDiHash(String diHash);

	boolean existsByCiHash(String ciHash);

	void updateProfile(@Param("userId") Long userId, @Param("phoneNumber") String phoneNumber);

	// 유저당 fcm_token 컬럼 하나뿐이라(테이블에 기기 목록 개념이 없음), 가장 최근에 로그인한
	// 기기가 이전 값을 덮어쓴다 - 여러 기기를 동시에 못 쓴다는 뜻이지만, 지금 요구사항이 딱 그
	// 정도라 별도 기기 테이블 없이 이렇게 간다.
	void updateFcmToken(@Param("userId") Long userId, @Param("fcmToken") String fcmToken);

	void updatePinHash(@Param("userId") Long userId, @Param("pinHash") String pinHash);

	void updatePasswordHash(@Param("userId") Long userId, @Param("loginPasswordHash") String loginPasswordHash);

	void softDeleteAndAnonymize(Long userId);
}
