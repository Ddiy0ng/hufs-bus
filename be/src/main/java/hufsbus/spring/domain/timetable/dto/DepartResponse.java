package hufsbus.spring.domain.timetable.dto;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.timetable.entity.Timetable;
import lombok.Getter;

@Getter
public class DepartResponse {
    private final Long timetableId;
    private final Long busId;
    private final String actualDepartureTime;
    private final String busStatus;

    private DepartResponse(Long timetableId, Long busId, String actualDepartureTime, String busStatus) {
        this.timetableId = timetableId;
        this.busId = busId;
        this.actualDepartureTime = actualDepartureTime;
        this.busStatus = busStatus;
    }

    public static DepartResponse of(Timetable timetable, Bus bus) {
        return new DepartResponse(
                timetable.getId(),
                bus.getId(),
                timetable.getActualDepartureTime() != null ? timetable.getActualDepartureTime().toString() : null,
                bus.getStatus().name()
        );
    }
}
