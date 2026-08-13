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

	void updatePinHash(@Param("userId") Long userId, @Param("pinHash") String pinHash);

	void updatePasswordHash(@Param("userId") Long userId, @Param("loginPasswordHash") String loginPasswordHash);

	void softDeleteAndAnonymize(Long userId);
}
