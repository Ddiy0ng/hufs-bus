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

    private SseLocationEvent(Long busId, Double latitude, Double longitude, Integer currentSeats, String status) {
        this.busId = busId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentSeats = currentSeats;
        this.status = status;
    }

    public static SseLocationEvent of(Bus bus, DriverLocation location) {
        return new SseLocationEvent(bus.getId(), location.getLatitude(), location.getLongitude(),
                bus.getCurrentSeats(), bus.getStatus().name());
    }
}
