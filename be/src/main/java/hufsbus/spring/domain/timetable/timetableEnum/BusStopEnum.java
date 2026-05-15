package hufsbus.spring.domain.timetable.timetableEnum;

import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BusStopEnum {

    // INNER UNIV
    DOLMEN("지석묘"),
    DORM("기숙사"),
    LIBRARY("도서관"),
    EOMUN_HALL("어문관"),
    INGYEONG_HALL("인문경상관"),
    GYOYANG_HALL("교양관"),
    ENGINEERING_HALL("공학관"),
    CENTENNIAL_HALL("백년관"),

    //OUTER UNIV
    PANGYO_STATION("판교역"),
    SEONGNAM_STATION("성남역"),
    SEOHYEON_STATION("서현역"),
    HUFS_GLOBAL("한국외대 글로벌캠퍼스"),
    GYEONGGI_GWANGJU_STATION_TAXI_STAND_AT_EXIT1("경기광주역 1번출구 택시 승강장");


    private final String busStopName;

    public static BusStopEnum from(String busStopName) {
        String noBlankValue = busStopName.trim();

        for(BusStopEnum busStopEnum : BusStopEnum.values()) {
            if (busStopEnum.name().equals(noBlankValue) || busStopEnum.busStopName.equals(noBlankValue)) {
                return busStopEnum;
            }
        }

        throw new CustomException(ErrorCode.NO_BUS_STOP_ENUM);
    }
}
