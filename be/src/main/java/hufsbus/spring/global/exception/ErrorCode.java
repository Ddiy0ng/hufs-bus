package hufsbus.spring.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    //500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    EXCEL_PARSING_EXCEPTION(HttpStatus.BAD_REQUEST, "엑셀 파일 등록 중 오류입니다."),
    NO_UP_DOWN_ENUM(HttpStatus.BAD_REQUEST, "상하행에 대한 값이어야 합니다."),
    NO_IN_OUT_ENUM(HttpStatus.BAD_REQUEST, "교내외에 대한 값이어야 합니다."),
    NO_BUS_STOP_ENUM(HttpStatus.BAD_REQUEST, "정확한 버스 정류장 이름이어야 합니다."),
    ROUTE_PARSING_EXCEPTION(HttpStatus.BAD_REQUEST, "경로 파싱 중 에러가 발생했습니다."),
    PARSED_ROUTE_EMPTY_EXCEPTION(HttpStatus.BAD_REQUEST, "파싱한 경로값이 EMPTY 상태입니다."),
    CREATE_TIMETABLE_EXCEPTION(HttpStatus.BAD_REQUEST, "시간표 생성 중 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
