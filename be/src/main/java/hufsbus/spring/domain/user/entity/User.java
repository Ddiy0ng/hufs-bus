package hufsbus.spring.domain.user.entity;

import hufsbus.spring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    public enum UserRole { PASSENGER, DRIVER, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "is_privacy_term_agree")
    private boolean privacyTermAgree;

    @Column(name = "is_service_term_agree")
    private boolean serviceTermAgree;

    public static User of(String email, String encodedPassword, boolean privacyTermAgree, boolean serviceTermAgree) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.role = UserRole.PASSENGER;
        user.privacyTermAgree = privacyTermAgree;
        user.serviceTermAgree = serviceTermAgree;
        return user;
    }
}
