package hufsbus.spring.domain.user.dto;

import hufsbus.spring.domain.user.entity.User;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final Long userId;
    private final String email;
    private final String role;

    private LoginResponse(String accessToken, Long userId, String email, String role) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public static LoginResponse of(String accessToken, User user) {
        return new LoginResponse(
                accessToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
