package hufsbus.spring.domain.timetable.dto;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.timetable.entity.Timetable;
import lombok.Getter;

@Getter
public class DepartResponse {
    private final Long timetableId;
    private final String actualDepartureTime;
    private final String busStatus;

    private DepartResponse(Long timetableId, String actualDepartureTime, String busStatus) {
        this.timetableId = timetableId;
        this.actualDepartureTime = actualDepartureTime;
        this.busStatus = busStatus;
    }

    public static DepartResponse of(Timetable timetable, Bus bus) {
        return new DepartResponse(
                timetable.getId(),
                timetable.getActualDepartureTime().toString(),
                bus.getStatus().name()
        );
    }
}
