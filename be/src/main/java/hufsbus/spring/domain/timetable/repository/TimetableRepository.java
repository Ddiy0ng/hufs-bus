package hufsbus.spring.domain.timetable.repository;

import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    Boolean existsByBusRouteAndDepartAt(BusRoute busRoute, LocalTime departAt);
}
