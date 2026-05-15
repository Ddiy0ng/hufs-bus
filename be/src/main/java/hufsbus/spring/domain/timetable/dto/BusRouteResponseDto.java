package hufsbus.spring.domain.timetable.dto;

import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public class BusRouteResponseDto {

    private Long routeId;
    private String inOutCampus;
    private String startStop;
    private String endStop;
    private String route;

    public static BusRouteResponseDto of(BusRoute busRoute, List<BusStop> busStopList) {

        BusRouteResponseDto busRouteResponseDto = new BusRouteResponseDto();
        busRouteResponseDto.routeId = busRoute.getId();
        busRouteResponseDto.inOutCampus = busRoute.getInOutCampus().getInOutCampus();
        busRouteResponseDto.startStop = busRoute.getStartStop().getBusStopName();
        busRouteResponseDto.endStop = busStopList.get(busStopList.size() - 1).getBusStop().getBusStopName();
        busRouteResponseDto.route = busRouteResponseDto.startStop + " → " + busRouteResponseDto.endStop;

        return busRouteResponseDto;
    }
}
