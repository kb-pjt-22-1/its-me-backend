package site.benepay.auth.domain.user.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.auth.domain.user.entity.User;

public interface UserMapper {

    void insert(User user);

    Optional<User> findByUserId(Long userId);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByDiHash(String diHash);

    void updateProfile(@Param("userId") Long userId, @Param("email") String email, @Param("phoneNumber") String phoneNumber);

    void updatePinHash(@Param("userId") Long userId, @Param("pinHash") String pinHash);

    void softDeleteAndAnonymize(Long userId);
}
