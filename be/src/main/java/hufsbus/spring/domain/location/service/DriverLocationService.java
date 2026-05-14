package hufsbus.spring.domain.location.service;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.bus.repository.BusRepository;
import hufsbus.spring.domain.location.dto.DriverLocationRequest;
import hufsbus.spring.domain.location.dto.DriverLocationResponse;
import hufsbus.spring.domain.location.entity.DriverLocation;
import hufsbus.spring.domain.location.repository.DriverLocationRepository;
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

    @Transactional
    public DriverLocationResponse saveLocation(DriverLocationRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        DriverLocation location = DriverLocation.builder()
                .bus(bus)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        return DriverLocationResponse.of(driverLocationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public DriverLocationResponse getLatestLocation(Long busId) {
        DriverLocation location = driverLocationRepository
                .findFirstByBusIdOrderByCreatedAtDesc(busId)
                .orElseThrow(() -> new CustomException(ErrorCode.LOCATION_NOT_FOUND));
        return DriverLocationResponse.of(location);
    }
}
