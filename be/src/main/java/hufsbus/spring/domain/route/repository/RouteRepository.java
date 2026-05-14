package hufsbus.spring.domain.route.repository;

import hufsbus.spring.domain.route.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByRouteType(Route.RouteType routeType);
    List<Route> findByDirection(Route.Direction direction);
    List<Route> findByRouteTypeAndDirection(Route.RouteType routeType, Route.Direction direction);
}
