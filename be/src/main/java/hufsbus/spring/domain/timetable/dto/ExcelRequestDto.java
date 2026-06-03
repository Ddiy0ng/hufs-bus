package hufsbus.spring.domain.timetable.dto;

import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Builder
@Getter
@AllArgsConstructor
public class ExcelRequestDto {

    @NotNull(message = "출발 시간을 입력해 주세요")
    private LocalTime departAt;

    @NotNull(message = "교내/교외 여부를 입력해 주세요")
    private InOutCampusEnum inOutCampus;

    @NotNull(message = "하행/상행 여부를 입력해 주세요")
    private BusWayEnum busWay;

    @NotNull(message = "기점 정류장을 입력해 주세요")
    private BusStopEnum startStop;

    @NotNull(message = "경로를 입력해 주세요")
    private String route;
}
