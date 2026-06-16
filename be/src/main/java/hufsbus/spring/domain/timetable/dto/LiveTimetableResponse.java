package hufsbus.spring.domain.timetable.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LiveTimetableResponse {
    private Long timetableId;
    private String plannedDepartureTime;
    private String actualDepartureTime;
    private Integer currentSeats;
    private String status;
    private LocationInfo currentLocation;
    private List<StopEta> stops;

    @Getter
    @Builder
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Builder
    public static class StopEta {
        private String stopName;
        private String eta;
        private Integer sequence;
    }
}
