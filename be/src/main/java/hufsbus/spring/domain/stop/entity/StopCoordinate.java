package hufsbus.spring.domain.stop.entity;

import hufsbus.spring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "stop_coordinate")
public class StopCoordinate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_name", nullable = false)
    private String stopName;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Builder
    public StopCoordinate(String stopName, Double latitude, Double longitude) {
        this.stopName = stopName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
