package hufsbus.spring.domain.currentlocation.service;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.bus.repository.BusRepository;
import hufsbus.spring.domain.currentlocation.dto.DriverLocationRequest;
import hufsbus.spring.domain.currentlocation.dto.DriverLocationResponse;
import hufsbus.spring.domain.currentlocation.dto.SseLocationEvent;
import hufsbus.spring.domain.currentlocation.entity.DriverLocation;
import hufsbus.spring.domain.currentlocation.repository.DriverLocationRepository;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.repository.BusStopRepository;
import hufsbus.spring.domain.timetable.service.SseEmitterService;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private static final double ARRIVAL_RADIUS_METERS = 100.0;

    private final DriverLocationRepository driverLocationRepository;
    private final BusRepository busRepository;
    private final SseEmitterService sseEmitterService;
    private final BusStopRepository busStopRepository;

    // timetableId → 현재 정류장 순서
    private final Map<Long, Integer> currentStopSequenceMap = new ConcurrentHashMap<>();

    @Transactional
    public DriverLocationResponse saveLocation(DriverLocationRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        DriverLocation location = DriverLocation.builder()
                .bus(bus)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        DriverLocation saved = driverLocationRepository.save(location);

        if (bus.getTimetable() != null) {
            Long timetableId = bus.getTimetable().getId();

            List<BusStop> stops = busStopRepository
                    .findByBusRouteOrderByStopOrderAsc(bus.getTimetable().getBusRoute());

            BusStop currentStop = resolveCurrentStop(timetableId, stops,
                    request.getLatitude(), request.getLongitude());

            Integer stopSequence = currentStop != null ? currentStop.getStopOrder() : null;
            String stopName = currentStop != null ? currentStop.getBusStop().getBusStopName() : null;

            sseEmitterService.broadcast(timetableId, "location-update",
                    SseLocationEvent.of(bus, saved, stopSequence, stopName));
        }

        return DriverLocationResponse.of(saved);
    }

    private BusStop resolveCurrentStop(Long timetableId, List<BusStop> stops, double lat, double lng) {
        if (stops.isEmpty()) return null;

        int trackedSequence = currentStopSequenceMap.getOrDefault(timetableId, stops.get(0).getStopOrder());

        for (BusStop stop : stops) {
            if (stop.getStopOrder() <= trackedSequence) continue;
            if (stop.getLatitude() == null || stop.getLongitude() == null) continue;

            double dist = haversine(lat, lng, stop.getLatitude(), stop.getLongitude());
            if (dist <= ARRIVAL_RADIUS_METERS) {
                currentStopSequenceMap.put(timetableId, stop.getStopOrder());
                trackedSequence = stop.getStopOrder();
            }
            break; // 한 번에 하나의 정류장만 체크
        }

        int finalSequence = trackedSequence;
        return stops.stream()
                .filter(s -> s.getStopOrder() == finalSequence)
                .findFirst()
                .orElse(stops.get(0));
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Transactional(readOnly = true)
    public DriverLocationResponse getLatestLocation(Long busId) {
        DriverLocation location = driverLocationRepository
                .findFirstByBusIdOrderByCreatedAtDesc(busId)
                .orElseThrow(() -> new CustomException(ErrorCode.LOCATION_NOT_FOUND));
        return DriverLocationResponse.of(location);
    }
}
