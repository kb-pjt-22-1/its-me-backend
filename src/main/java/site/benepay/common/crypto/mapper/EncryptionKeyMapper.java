package site.benepay.common.crypto.mapper;

import org.apache.ibatis.annotations.Param;
import site.benepay.common.crypto.EncryptionKey;

import java.util.Optional;

public interface EncryptionKeyMapper {

    Optional<EncryptionKey> findActiveByAlias(@Param("keyAlias") String keyAlias);
}
