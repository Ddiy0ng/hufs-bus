package hufsbus.spring.global.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApiResponseDto<T> {

    private final String message;
    private final T data;
    private final LocalDateTime localDateTime;

    @Builder
    public ApiResponseDto (String message, T data) {
        this.message = message;
        this.data = data;
        this.localDateTime = LocalDateTime.now();
    }

    // 성공: 응답 데이터 있는 경우
    public static <T> ApiResponseDto<T> of (T data, String message) {
        return ApiResponseDto.<T>builder()
                .message(message)
                .data(data)
                .build();
    }
    // 성공: 응답 데이터 없는 경우
    public static <T> ApiResponseDto<T> success (String message) {
        return ApiResponseDto.<T>builder()
                .message(message)
                .build();
    }
    // 실패한 경우
    public static <T> ApiResponseDto<T> fail (String message) {
        return ApiResponseDto.<T>builder()
                .message(message)
                .build();
    }
}
