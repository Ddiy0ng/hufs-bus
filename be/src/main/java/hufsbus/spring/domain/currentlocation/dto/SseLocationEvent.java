package hufsbus.spring.domain.currentlocation.dto;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.domain.currentlocation.entity.DriverLocation;
import lombok.Getter;

@Getter
public class SseLocationEvent {

    private final Long busId;
    private final Double latitude;
    private final Double longitude;
    private final Integer currentSeats;
    private final String status;
    private final Integer currentStopSequence;
    private final String currentStopName;

    private SseLocationEvent(Long busId, Double latitude, Double longitude, Integer currentSeats, String status,
                              Integer currentStopSequence, String currentStopName) {
        this.busId = busId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentSeats = currentSeats;
        this.status = status;
        this.currentStopSequence = currentStopSequence;
        this.currentStopName = currentStopName;
    }

    public static SseLocationEvent of(Bus bus, DriverLocation location,
                                      Integer currentStopSequence, String currentStopName) {
        return new SseLocationEvent(bus.getId(), location.getLatitude(), location.getLongitude(),
                bus.getCurrentSeats(), bus.getStatus().name(), currentStopSequence, currentStopName);
    }
}
