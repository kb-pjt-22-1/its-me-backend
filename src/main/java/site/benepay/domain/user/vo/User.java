package site.benepay.domain.user.vo;

import java.time.LocalDateTime;

/**
 * Wraps a single row of the users table. Identity/audit columns are fixed at creation and
 * expose no setter; only columns the application actually updates after creation
 * (phoneNumber, pinHash, deleted - see UserMapper.xml) do.
 */
public class User {

    private Long userId;
    private String loginId;
    private String passwordHash;
    private String pinHash;
    private String ciHash;
    private String name;
    private String phoneNumber;
    private Role role;
    private String di;
    private LocalDateTime createdAt;
    private Boolean deleted;

    public User() {
    }

    public User(Long userId, String loginId, String passwordHash, String pinHash,
                 String ciHash, String name, String phoneNumber, Role role, String di,
                 LocalDateTime createdAt, boolean deleted) {
        this.userId = userId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.pinHash = pinHash;
        this.ciHash = ciHash;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.di = di;
        this.createdAt = createdAt;
        this.deleted = deleted;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPinHash() {
        return pinHash;
    }

    public String getCiHash() {
        return ciHash;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Role getRole() {
        return role;
    }

    public String getDi() {
        return di;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isDeleted() {
        return deleted;
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
        private String passwordHash;
        private String pinHash;
        private String ciHash;
        private String name;
        private String phoneNumber;
        private Role role;
        private String di;
        private LocalDateTime createdAt;
        private boolean deleted;

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

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder pinHash(String pinHash) {
            this.pinHash = pinHash;
            return this;
        }

        public Builder ciHash(String ciHash) {
            this.ciHash = ciHash;
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

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder di(String di) {
            this.di = di;
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

        public User build() {
            return new User(userId, loginId, passwordHash, pinHash, ciHash, name,
                    phoneNumber, role, di, createdAt, deleted);
        }
    }
}
