package hufsbus.spring.domain.timetable.repository;

import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    Boolean existsByBusRouteAndDepartAt(BusRoute busRoute, LocalTime startTime);

    @Query("""
            SELECT timetable
            FROM Timetable timetable
            JOIN FETCH timetable.busRoute busRoute
            WHERE busRoute.id = :routeId
                AND timetable.departAt >= :startTime
                AND timetable.departAt < :endTime
            ORDER BY timetable.departAt ASC
            """)
    List<Timetable> findByRouteIdAndTimes(
            @Param("routeId") Long routeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
            );
}
