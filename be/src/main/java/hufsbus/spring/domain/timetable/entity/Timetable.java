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

    // 기점 출발 시간
    @Column(name = "depart_at", nullable = false)
    private LocalTime departAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_route_id", nullable = false)
    private BusRoute busRoute;

    public static Timetable of(LocalTime departAt, BusRoute busRoute) {

        Timetable timetable = new Timetable();
        timetable.departAt = departAt;
        timetable.busRoute = busRoute;

        return timetable;
    }
}
