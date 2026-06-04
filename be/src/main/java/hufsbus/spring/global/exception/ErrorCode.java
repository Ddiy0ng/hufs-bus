package hufsbus.spring.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    //500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    TIMETABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 시간표를 찾을 수 없습니다."),
    BUS_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 버스를 찾을 수 없습니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "위치 정보를 찾을 수 없습니다."),
    SEAT_UNAVAILABLE(HttpStatus.BAD_REQUEST, "여석이 없습니다."),
    NO_PASSENGER(HttpStatus.BAD_REQUEST, "버스에 탑승한 승객이 없습니다."),
    EXCEL_PARSING_EXCEPTION(HttpStatus.BAD_REQUEST, "엑셀 파일 등록 중 오류입니다."),
    NO_UP_DOWN_ENUM(HttpStatus.BAD_REQUEST, "상하행에 대한 값이어야 합니다."),
    NO_IN_OUT_ENUM(HttpStatus.BAD_REQUEST, "교내외에 대한 값이어야 합니다."),
    NO_BUS_STOP_ENUM(HttpStatus.BAD_REQUEST, "정확한 버스 정류장 이름이어야 합니다."),
    ROUTE_PARSING_EXCEPTION(HttpStatus.BAD_REQUEST, "경로 파싱 중 에러가 발생했습니다."),
    PARSED_ROUTE_EMPTY_EXCEPTION(HttpStatus.BAD_REQUEST, "파싱한 경로값이 EMPTY 상태입니다."),
    PARSE_TIMETABLE_EXCEPTION(HttpStatus.BAD_REQUEST, "시간표 생성 중 에러가 발생했습니다."),
    ROUTES_NOT_EXISTS_EXCEPTION(HttpStatus.BAD_REQUEST, "노선이 존재하지 않습니다."),
    TIMETABLE_NOT_EXIST_EXCEPTION(HttpStatus.BAD_REQUEST, "시간표가 존재하지 않습니다."),
    USER_NOT_EXIST_EXCEPTION(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자입니다."),
    CREATE_TIMETABLE_EXCEPTION(HttpStatus.BAD_REQUEST, "시간표 생성 중 에러가 발생했습니다."),
    NO_ROUTES_EXISTS_EXCEPTION(HttpStatus.BAD_REQUEST, "노선이 존재하지 않습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "한국외대(@ac.kr) 이메일만 허용됩니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
