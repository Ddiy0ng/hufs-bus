package hufsbus.spring.domain.bus.dto;

import hufsbus.spring.domain.bus.entity.Bus;
import lombok.Getter;

@Getter
public class SeatUpdateEvent {

    private final Long timetableId;
    private final Long busId;
    private final Integer totalSeats;
    private final Integer currentSeats;
    private final Integer remainingSeats;
    private final String tagType;

    private SeatUpdateEvent(Long timetableId, Long busId, Integer totalSeats, Integer currentSeats,
                            Integer remainingSeats, String tagType) {
        this.timetableId = timetableId;
        this.busId = busId;
        this.totalSeats = totalSeats;
        this.currentSeats = currentSeats;
        this.remainingSeats = remainingSeats;
        this.tagType = tagType;
    }

    public static SeatUpdateEvent of(Long timetableId, Bus bus, BusTagRequest.TagType tagType) {
        return new SeatUpdateEvent(
                timetableId,
                bus.getId(),
                bus.getTotalSeats(),
                bus.getCurrentSeats(),
                bus.getTotalSeats() - bus.getCurrentSeats(),
                tagType.name()
        );
    }
}
