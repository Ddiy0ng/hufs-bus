package hufsbus.spring.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    //500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // 404 NOT_FOUND: 예시입니다~
    // ,를 통해서 상수를 추가하세요~
    EXAMPLE_NOT_FOUND_ERROR(HttpStatus.NOT_FOUND, "404 NOT_FOUND 예시입니다~"),
    EXAMPLE_BAD_REQUEST_ERROR(HttpStatus.BAD_REQUEST, "400 BAD_REQUEST 예시입니다~");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
