package hufsbus.spring.domain.timetable.repository;

import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute,Long> {

    List<BusRoute> findByInOutCampusOrderByIdAsc(InOutCampusEnum inOutCampus);

    Optional<BusRoute> findFirstByInOutCampusOrderByIdAsc(
            InOutCampusEnum inOutCampus
    );

    List<BusRoute> findByInOutCampusAndBusWayAndStartStop(
            InOutCampusEnum inOutCampus,
            BusWayEnum busWay,
            BusStopEnum startStop
    );
}
