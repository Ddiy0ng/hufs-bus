package hufsbus.spring.domain.favorite.dto;

import hufsbus.spring.domain.favorite.entity.Favorite;
import hufsbus.spring.domain.favorite.favoriteEnum.DayEnum;
import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.entity.Timetable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;

@NoArgsConstructor
@Getter
public class FavoriteResponseDto {

    private Long favoriteId;
    private Long timetableId;
    private DayEnum day;
    private String departAt;
    private String route;

    public static FavoriteResponseDto of(Favorite favorite, List<BusStop> busStopList) {

        Timetable timetable = favorite.getTimetable();
        BusRoute busRoute = timetable.getBusRoute();
        String startStop = busRoute.getStartStop().getBusStopName();
        String endStop = busStopList.get(busStopList.size() - 1).getBusStop().getBusStopName();

        FavoriteResponseDto favoriteResponseDto = new FavoriteResponseDto();

        favoriteResponseDto.favoriteId = favorite.getId();
        favoriteResponseDto.timetableId = timetable.getId();
        favoriteResponseDto.day = favorite.getDay();
        favoriteResponseDto.departAt = timetable.getDepartAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        favoriteResponseDto.route = startStop + " → " + endStop;

        return favoriteResponseDto;
    }

}
