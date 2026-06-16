package hufsbus.spring.domain.bus.dto;

import hufsbus.spring.domain.bus.entity.Bus;
import lombok.Getter;

@Getter
public class BusResponse {
    private final Long busId;
    private final Integer totalSeats;
    private final Integer currentSeats;
    private final String status;

    private BusResponse(Long busId, Integer totalSeats, Integer currentSeats, String status) {
        this.busId = busId;
        this.totalSeats = totalSeats;
        this.currentSeats = currentSeats;
        this.status = status;
    }

    public static BusResponse of(Bus bus) {
        return new BusResponse(bus.getId(), bus.getTotalSeats(), bus.getCurrentSeats(), bus.getStatus().name());
    }
}
