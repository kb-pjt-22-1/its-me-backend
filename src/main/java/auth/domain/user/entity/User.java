package site.benepay.auth.domain.user.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long userId;
    private String email;
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
}
