package hufsbus.spring.domain.timetable.dto;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.timetable.entity.Timetable;
import lombok.Getter;

@Getter
public class TimetableResponse {
    private final Long timetableId;
    private final String departureTime;
    private final String actualDepartureTime;
    private final Integer roundNo;
    private final Integer currentSeats;
    private final String status;

    private TimetableResponse(Long timetableId, String departureTime, String actualDepartureTime,
                               Integer roundNo, Integer currentSeats, String status) {
        this.timetableId = timetableId;
        this.departureTime = departureTime;
        this.actualDepartureTime = actualDepartureTime;
        this.roundNo = roundNo;
        this.currentSeats = currentSeats;
        this.status = status;
    }

    public static TimetableResponse of(Timetable timetable, Bus bus) {
        return new TimetableResponse(
                timetable.getId(),
                timetable.getDepartureTime().toString(),
                timetable.getActualDepartureTime() != null ? timetable.getActualDepartureTime().toString() : null,
                timetable.getRoundNo(),
                bus != null ? bus.getCurrentSeats() : null,
                bus != null ? bus.getStatus().name() : "WAITING"
        );
    }
}
