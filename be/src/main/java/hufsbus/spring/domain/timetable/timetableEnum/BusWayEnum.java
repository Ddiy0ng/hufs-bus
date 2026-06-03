package hufsbus.spring.domain.timetable.timetableEnum;

import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BusWayEnum {

    UP_BOUND_LINE("상행"),
    DOWN_BOUND_lINE("하행");

    private final String busWay;

    public static BusWayEnum from(String busWay) {
        String noBlankValue = busWay.trim();

        for(BusWayEnum busWayEnum : BusWayEnum.values()) {
            if (busWayEnum.name().equals(noBlankValue) || busWayEnum.busWay.equals(noBlankValue)) {
                return busWayEnum;
            }
        }

        throw new CustomException(ErrorCode.NO_UP_DOWN_ENUM);
    }
}
