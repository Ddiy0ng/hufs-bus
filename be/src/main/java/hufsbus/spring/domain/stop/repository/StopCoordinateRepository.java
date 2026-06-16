package hufsbus.spring.domain.stop.repository;

import hufsbus.spring.domain.stop.entity.StopCoordinate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StopCoordinateRepository extends JpaRepository<StopCoordinate, Long> {

    List<StopCoordinate> findByStopName(String stopName);
}
