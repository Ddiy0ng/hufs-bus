package hufsbus.spring.domain.location.dto;

import hufsbus.spring.domain.location.entity.DriverLocation;
import lombok.Getter;

@Getter
public class DriverLocationResponse {
    private final Long busId;
    private final Double latitude;
    private final Double longitude;
    private final String recordedAt;

    private DriverLocationResponse(Long busId, Double latitude, Double longitude, String recordedAt) {
        this.busId = busId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = recordedAt;
    }

    public static DriverLocationResponse of(DriverLocation location) {
        return new DriverLocationResponse(
                location.getBus().getId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getUpdatedAt().toString()
        );
    }
}
