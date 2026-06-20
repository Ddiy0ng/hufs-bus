package hufsbus.spring.domain.bus.entity;

import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "bus")
public class Bus extends BaseEntity {

    public enum BusStatus { WAITING, RUNNING, DONE }

    public static final int DEFAULT_SEATS = 45;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bus_number", nullable = false, unique = true)
    private String busNumber;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "current_seats", nullable = false)
    private Integer currentSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BusStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id")
    private Timetable timetable;

    @Builder
    public Bus(String busNumber, Integer totalSeats, Integer currentSeats, BusStatus status, Timetable timetable) {
        this.busNumber = busNumber;
        this.totalSeats = totalSeats;
        this.currentSeats = currentSeats;
        this.status = status;
        this.timetable = timetable;
    }

    public void board() {
        this.currentSeats++;
    }

    public void alight() {
        this.currentSeats--;
    }

    public void startRunning() {
        this.status = BusStatus.RUNNING;
    }
}
