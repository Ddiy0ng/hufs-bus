package hufsbus.spring.domain.timetable.timetableEnum;

import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum InOutCampusEnum {

    IN_CAMPUS("교내"),
    OUT_CAMPUS("교외");

    private final String inOutCampus;

    public static InOutCampusEnum from(String inOutCampus) {
        String noBlankValue = inOutCampus.trim();

        for(InOutCampusEnum inOutCampusEnum : InOutCampusEnum.values()) {
            if (inOutCampusEnum.name().equals(noBlankValue) || inOutCampusEnum.inOutCampus.equals(noBlankValue)) {
                return inOutCampusEnum;
            }
        }

        throw new CustomException(ErrorCode.NO_IN_OUT_ENUM);
    }
}
