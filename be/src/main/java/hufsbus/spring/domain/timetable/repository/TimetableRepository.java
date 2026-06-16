package hufsbus.spring.domain.timetable.repository;

import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.Timetable;
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

    long countByBusRouteAndDepartAt(BusRoute busRoute, LocalTime departAt);

    @Query("""
            SELECT timetable
            FROM Timetable timetable
            JOIN FETCH timetable.busRoute busRoute
            WHERE busRoute.inOutCampus = :inOutCampus
                AND (:routeId IS NULL OR busRoute.id = :routeId)
                AND (:startTime IS NULL OR timetable.departAt >= :startTime)
                AND (:endTime IS NULL OR timetable.departAt < :endTime)
            ORDER BY busRoute.id ASC, timetable.departAt ASC
            """)
    List<Timetable> searchTimetables(
            @Param("inOutCampus") InOutCampusEnum inOutCampus,
            @Param("routeId") Long routeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
