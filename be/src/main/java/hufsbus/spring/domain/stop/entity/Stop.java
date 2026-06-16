package hufsbus.spring.domain.stop.entity;

import hufsbus.spring.domain.route.entity.Route;
import hufsbus.spring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "stop")
public class Stop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "stop_name", nullable = false)
    private String stopName;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Builder
    public Stop(Route route, String stopName, Integer sequence, Double latitude, Double longitude) {
        this.route = route;
        this.stopName = stopName;
        this.sequence = sequence;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
