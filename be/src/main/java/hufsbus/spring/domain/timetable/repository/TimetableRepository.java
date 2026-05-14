package hufsbus.spring.domain.timetable.repository;

import hufsbus.spring.domain.route.entity.Route;
import hufsbus.spring.domain.timetable.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    @Query("SELECT t FROM Timetable t WHERE t.route.id = :routeId AND HOUR(t.departureTime) = :hour AND t.isActive = true")
    List<Timetable> findByRouteIdAndHour(@Param("routeId") Long routeId, @Param("hour") int hour);

    @Query("SELECT t FROM Timetable t WHERE t.route.routeType = :routeType AND HOUR(t.departureTime) = :hour AND t.isActive = true")
    List<Timetable> findByRouteTypeAndHour(@Param("routeType") Route.RouteType routeType, @Param("hour") int hour);

    @Query("SELECT t FROM Timetable t WHERE HOUR(t.departureTime) = :hour AND t.isActive = true")
    List<Timetable> findByHour(@Param("hour") int hour);
}
