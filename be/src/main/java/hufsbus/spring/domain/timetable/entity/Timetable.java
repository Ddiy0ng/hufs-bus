package hufsbus.spring.domain.timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@Entity
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "depart_at", nullable = false)
    private LocalTime departAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_route_id", nullable = false)
    private BusRoute busRoute;

    @Column(name = "actual_departure_time")
    private LocalTime actualDepartureTime;

    public void depart(LocalTime actualTime) {
        this.actualDepartureTime = actualTime;
    }

    public static Timetable of(LocalTime departAt, BusRoute busRoute) {
        Timetable timetable = new Timetable();
        timetable.departAt = departAt;
        timetable.busRoute = busRoute;
        return timetable;
    }
}
