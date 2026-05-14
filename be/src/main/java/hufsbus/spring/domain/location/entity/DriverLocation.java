package hufsbus.spring.domain.location.entity;

import hufsbus.spring.domain.bus.entity.Bus;
import hufsbus.spring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "driver_location")
public class DriverLocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Builder
    public DriverLocation(Bus bus, Double latitude, Double longitude) {
        this.bus = bus;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
