package hufsbus.spring.domain.timetable.dto;

import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.entity.Timetable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
public class TimetableResponseDto {

    private Long timetableId;
    private Long routeId;
    private String inOutCampus;
    private String startStop;
    private String endStop;
    private String departAt;
    private List<String> routeList;

    public static TimetableResponseDto of(Timetable timetable, List<BusStop> busStopList) {

        BusRoute busRoute = timetable.getBusRoute();
        List<String> route = new ArrayList<>();

        for (BusStop busStop : busStopList)
            route.add(busStop.getBusStop().getBusStopName());

        TimetableResponseDto timetableResponseDto = new TimetableResponseDto();
        timetableResponseDto.timetableId = timetable.getId();
        timetableResponseDto.routeId = busRoute.getId();
        timetableResponseDto.inOutCampus = busRoute.getInOutCampus().getInOutCampus();
        timetableResponseDto.startStop = busRoute.getStartStop().getBusStopName();
        timetableResponseDto.endStop = busRoute.getStartStop().getBusStopName();
        timetableResponseDto.departAt = timetable.getDepartAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        timetableResponseDto.routeList = route;

        return timetableResponseDto;
    };
}
