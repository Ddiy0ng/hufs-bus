package hufsbus.spring.domain.timetable.entity;

import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class BusStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 버정
    @Enumerated(EnumType.STRING)
    @Column(name = "bus_stop", nullable = false)
    private BusStopEnum busStop;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_route_id", nullable = false)
    private BusRoute busRoute;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public static BusStop of(BusStopEnum busStopEnum, Integer stopOrder, BusRoute busRoute) {
        BusStop busStop = new BusStop();
        busStop.busStop = busStopEnum;
        busStop.stopOrder = stopOrder;
        busStop.busRoute = busRoute;
        return busStop;
    }
}


