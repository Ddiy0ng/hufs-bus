package hufsbus.spring.domain.timetable.entity;

import hufsbus.spring.domain.route.entity.Route;
import hufsbus.spring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "timetable")
public class Timetable extends BaseEntity {

    public enum DayType { WEEKDAY, WEEKEND }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

    @Column(name = "round_no")
    private Integer roundNo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "actual_departure_time")
    private LocalTime actualDepartureTime;

    @Builder
    public Timetable(Route route, LocalTime departureTime, DayType dayType, Integer roundNo, Boolean isActive) {
        this.route = route;
        this.departureTime = departureTime;
        this.dayType = dayType;
        this.roundNo = roundNo;
        this.isActive = isActive != null ? isActive : true;
    }

    public void depart(LocalTime actualTime) {
        this.actualDepartureTime = actualTime;
    }
}