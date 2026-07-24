package site.benepay.domain.user.mapper;

import org.apache.ibatis.annotations.Param;
import site.benepay.domain.user.vo.User;

import java.util.Optional;

public interface UserMapper {

    void insert(User user);

    Optional<User> findByUserId(Long userId);

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByDiHash(String diHash);

    void updateProfile(@Param("userId") Long userId, @Param("phoneNumber") String phoneNumber);

    void updatePinHash(@Param("userId") Long userId, @Param("pinHash") String pinHash);

    void softDeleteAndAnonymize(Long userId);
}
