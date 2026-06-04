package hufsbus.spring.domain.bus.dto;

import hufsbus.spring.domain.bus.entity.Bus;
import lombok.Getter;

@Getter
public class BusTagResponse {
    private final Long busId;
    private final Integer totalSeats;
    private final Integer currentSeats;
    private final String tagType;

    private BusTagResponse(Long busId, Integer totalSeats, Integer currentSeats, String tagType) {
        this.busId = busId;
        this.totalSeats = totalSeats;
        this.currentSeats = currentSeats;
        this.tagType = tagType;
    }

    public static BusTagResponse of(Bus bus, BusTagRequest.TagType tagType) {
        return new BusTagResponse(bus.getId(), bus.getTotalSeats(), bus.getCurrentSeats(), tagType.name());
    }
}
