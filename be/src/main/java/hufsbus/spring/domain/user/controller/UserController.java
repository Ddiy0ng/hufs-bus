package hufsbus.spring.domain.user.controller;

import hufsbus.spring.domain.user.service.UserService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> withdraw(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponseDto.success("회원탈퇴 성공"));
    }
}
