package site.benepay.domain.user.vo;

import java.time.LocalDateTime;

/**
 * users 테이블의 한 행. 필드 순서는 스키마의 컬럼 순서를 따른다.
 *
 * <p>생성 시점에 확정되는 식별·감사 컬럼에는 setter를 두지 않는다. 생성 이후 애플리케이션이
 * 실제로 갱신하는 컬럼(phoneNumber, pinHash, deleted - UserMapper.xml 참고)에만 setter가 있다.
 */
public class User {

    private Long userId;
    private String loginId;
    private String loginPasswordHash;
    private String pinHash;
    private String name;
    private String phoneNumber;
    // 컬럼이 CHAR(8) 'YYYYMMDD'이다. LocalDate로 받으면 TypeHandler를 따로 붙여야 하는데,
    // 날짜 연산을 하는 곳이 없어 저장 형식 그대로 두는 편이 단순하다.
    private String birthDate;
    private Role role;
    private String di;
    private String ciEncrypted;
    private LocalDateTime createdAt;
    private Boolean deleted;
    private String fcmToken;

    public User() {
    }

    public User(Long userId, String loginId, String loginPasswordHash, String pinHash,
                 String name, String phoneNumber, String birthDate, Role role, String di,
                 String ciEncrypted, LocalDateTime createdAt, boolean deleted, String fcmToken) {
        this.userId = userId;
        this.loginId = loginId;
        this.loginPasswordHash = loginPasswordHash;
        this.pinHash = pinHash;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.role = role;
        this.di = di;
        this.ciEncrypted = ciEncrypted;
        this.createdAt = createdAt;
        this.deleted = deleted;
        this.fcmToken = fcmToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getLoginPasswordHash() {
        return loginPasswordHash;
    }

    public String getPinHash() {
        return pinHash;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public Role getRole() {
        return role;
    }

    public String getDi() {
        return di;
    }

    public String getCiEncrypted() {
        return ciEncrypted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public static final class Builder {
        private Long userId;
        private String loginId;
        private String loginPasswordHash;
        private String pinHash;
        private String name;
        private String phoneNumber;
        private String birthDate;
        private Role role;
        private String di;
        private String ciEncrypted;
        private LocalDateTime createdAt;
        private boolean deleted;
        private String fcmToken;

        private Builder() {
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder loginId(String loginId) {
            this.loginId = loginId;
            return this;
        }

        public Builder loginPasswordHash(String loginPasswordHash) {
            this.loginPasswordHash = loginPasswordHash;
            return this;
        }

        public Builder pinHash(String pinHash) {
            this.pinHash = pinHash;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder birthDate(String birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder di(String di) {
            this.di = di;
            return this;
        }

        public Builder ciEncrypted(String ciEncrypted) {
            this.ciEncrypted = ciEncrypted;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder fcmToken(String fcmToken) {
            this.fcmToken = fcmToken;
            return this;
        }

        public User build() {
            return new User(userId, loginId, loginPasswordHash, pinHash, name, phoneNumber,
                    birthDate, role, di, ciEncrypted, createdAt, deleted, fcmToken);
        }
    }
}
