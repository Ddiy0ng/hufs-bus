package hufsbus.spring.domain.route.entity;

import hufsbus.spring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "route")
public class Route extends BaseEntity {

    public enum RouteType { INBOUND, OUTBOUND }
    public enum Direction { TO_CAMPUS, FROM_CAMPUS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_name", nullable = false)
    private String routeName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type")
    private RouteType routeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private Direction direction;

    @Builder
    public Route(String routeName, String description, RouteType routeType, Direction direction) {
        this.routeName = routeName;
        this.description = description;
        this.routeType = routeType;
        this.direction = direction;
    }
}
