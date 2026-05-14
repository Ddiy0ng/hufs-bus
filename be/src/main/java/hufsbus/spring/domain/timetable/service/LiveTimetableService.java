package hufsbus.spring.domain.timetable.service;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.bus.repository.BusRepository;
import hufsbus.spring.domain.location.entity.DriverLocation;
import hufsbus.spring.domain.location.repository.DriverLocationRepository;
import hufsbus.spring.domain.stop.entity.Stop;
import hufsbus.spring.domain.stop.repository.StopRepository;
import hufsbus.spring.domain.timetable.dto.LiveTimetableResponse;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.timetable.repository.TimetableRepository;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveTimetableService {

    private final TimetableRepository timetableRepository;
    private final BusRepository busRepository;
    private final DriverLocationRepository driverLocationRepository;
    private final StopRepository stopRepository;
    private final KakaoRouteService kakaoRouteService;

    public LiveTimetableResponse getLive(Long timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMETABLE_NOT_FOUND));

        Bus bus = busRepository.findByTimetableId(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        DriverLocation location = driverLocationRepository
                .findFirstByBusIdOrderByCreatedAtDesc(bus.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.LOCATION_NOT_FOUND));

        List<Stop> stops = stopRepository.findByRouteIdOrderBySequenceAsc(
                timetable.getRoute().getId());

        List<LiveTimetableResponse.StopEta> stopEtas = stops.stream()
                .map(stop -> {
                    int eta = kakaoRouteService.getEtaMinutes(
                            location.getLatitude(), location.getLongitude(),
                            stop.getLatitude(), stop.getLongitude()
                    );
                    String etaText = eta >= 0 ? eta + "분 후 도착" : "정보 없음";
                    return LiveTimetableResponse.StopEta.builder()
                            .stopName(stop.getStopName())
                            .eta(etaText)
                            .sequence(stop.getSequence())
                            .build();
                })
                .collect(Collectors.toList());

        return LiveTimetableResponse.builder()
                .timetableId(timetableId)
                .plannedDepartureTime(timetable.getDepartureTime().toString())
                .actualDepartureTime(timetable.getActualDepartureTime() != null
                        ? timetable.getActualDepartureTime().toString() : null)
                .currentSeats(bus.getCurrentSeats())
                .status(bus.getStatus().name())
                .currentLocation(LiveTimetableResponse.LocationInfo.builder()
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .build())
                .stops(stopEtas)
                .build();
    }
}
