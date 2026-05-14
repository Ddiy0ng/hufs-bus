package hufsbus.spring.domain.timetable.service;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.bus.repository.BusRepository;
import hufsbus.spring.domain.route.entity.Route;
import hufsbus.spring.domain.timetable.dto.DepartResponse;
import hufsbus.spring.domain.timetable.dto.TimetableResponse;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.timetable.repository.TimetableRepository;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableService {
    private final TimetableRepository timetableRepository;
    private final BusRepository busRepository;

    public List<TimetableResponse> getTimetables(Long routeId, Route.RouteType routeType, int hour) {
        List<Timetable> timetables;
        if (routeId != null) {
            timetables = timetableRepository.findByRouteIdAndHour(routeId, hour);
        } else if (routeType != null) {
            timetables = timetableRepository.findByRouteTypeAndHour(routeType, hour);
        } else {
            timetables = timetableRepository.findByHour(hour);
        }

        if (timetables.isEmpty()) {
            throw new CustomException(ErrorCode.TIMETABLE_NOT_FOUND);
        }

        List<Long> ids = timetables.stream().map(Timetable::getId).collect(Collectors.toList());
        Map<Long, Bus> busMap = busRepository.findByTimetableIdIn(ids).stream()
                .collect(Collectors.toMap(b -> b.getTimetable().getId(), b -> b));

        return timetables.stream()
                .map(t -> TimetableResponse.of(t, busMap.get(t.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public DepartResponse depart(Long timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMETABLE_NOT_FOUND));

        Bus bus = busRepository.findByTimetableId(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        timetable.depart(LocalTime.now());
        bus.startRunning();

        return DepartResponse.of(timetable, bus);
    }
}
