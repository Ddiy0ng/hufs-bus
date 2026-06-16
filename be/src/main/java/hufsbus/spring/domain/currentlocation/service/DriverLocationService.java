package hufsbus.spring.domain.currentlocation.service;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.bus.repository.BusRepository;
import hufsbus.spring.domain.currentlocation.dto.DriverLocationRequest;
import hufsbus.spring.domain.currentlocation.dto.DriverLocationResponse;
import hufsbus.spring.domain.currentlocation.dto.SseLocationEvent;
import hufsbus.spring.domain.currentlocation.entity.DriverLocation;
import hufsbus.spring.domain.currentlocation.repository.DriverLocationRepository;
import hufsbus.spring.domain.timetable.service.SseEmitterService;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private final DriverLocationRepository driverLocationRepository;
    private final BusRepository busRepository;
    private final SseEmitterService sseEmitterService;

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
            sseEmitterService.broadcast(timetableId, "location-update", SseLocationEvent.of(bus, saved));
        }

        return DriverLocationResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public DriverLocationResponse getLatestLocation(Long busId) {
        DriverLocation location = driverLocationRepository
                .findFirstByBusIdOrderByCreatedAtDesc(busId)
                .orElseThrow(() -> new CustomException(ErrorCode.LOCATION_NOT_FOUND));
        return DriverLocationResponse.of(location);
    }
}
