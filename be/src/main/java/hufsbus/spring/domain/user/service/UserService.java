package hufsbus.spring.domain.user.service;

import hufsbus.spring.domain.user.config.JwtUtil;
import hufsbus.spring.domain.user.dto.LoginRequest;
import hufsbus.spring.domain.user.dto.LoginResponse;
import hufsbus.spring.domain.user.dto.SignupRequest;
import hufsbus.spring.domain.user.entity.User;
import hufsbus.spring.domain.user.repository.UserRepository;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void signup(SignupRequest request) {
        if (!request.getEmail().endsWith("@hufs.ac.kr")) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        userRepository.save(User.of(request.getEmail(), passwordEncoder.encode(request.getPassword())));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return LoginResponse.of(jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name()), user);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}
