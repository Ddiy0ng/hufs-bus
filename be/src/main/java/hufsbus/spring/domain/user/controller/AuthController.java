package hufsbus.spring.domain.user.controller;

import hufsbus.spring.domain.user.dto.LoginRequest;
import hufsbus.spring.domain.user.dto.LoginResponse;
import hufsbus.spring.domain.user.dto.SignupRequest;
import hufsbus.spring.domain.user.service.UserService;
import hufsbus.spring.global.dto.ApiResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponseDto<Void>> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.ok(ApiResponseDto.success("회원가입 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponseDto.of(userService.login(request), "로그인 성공"));
    }
}
