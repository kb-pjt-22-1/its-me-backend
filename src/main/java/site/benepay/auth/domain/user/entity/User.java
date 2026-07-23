package site.benepay.auth.domain.user.entity;

import lombok.*;

import java.time.LocalDateTime;

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
