package hufsbus.spring.domain.timetable.repository;

import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusStopRepository extends JpaRepository<BusStop, Long> {

    @Query("select bs from BusStop bs where bs.busRoute in :busRoutes order by bs.busRoute.id asc, bs.stopOrder asc")
    List<BusStop> findAllByBusRoutes(@Param("busRoutes") List<BusRoute> busRoutes);
    List<BusStop> findByBusRouteOrderByStopOrderAsc(BusRoute busRoute);
}
