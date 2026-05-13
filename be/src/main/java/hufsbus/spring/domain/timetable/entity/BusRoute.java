package hufsbus.spring.domain.timetable.entity;

import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@Getter
@Entity
public class BusRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 교내외
    @Enumerated(EnumType.STRING)
    @Column(name = "in_out_campus", nullable = false)
    private InOutCampusEnum inOutCampus;

    // 상하행
    @Enumerated(EnumType.STRING)
    @Column(name = "bus_way", nullable = false)
    private BusWayEnum busWay;

    //기점
    @Enumerated(EnumType.STRING)
    @Column(name ="start_stop", nullable = false)
    private BusStopEnum startStop;

    public static BusRoute of(InOutCampusEnum inOutCampus, BusWayEnum busWay, BusStopEnum startStop) {

        BusRoute busRoute = new BusRoute();
        busRoute.inOutCampus = inOutCampus;
        busRoute.busWay = busWay;
        busRoute.startStop = startStop;

        return busRoute;
    }
}
