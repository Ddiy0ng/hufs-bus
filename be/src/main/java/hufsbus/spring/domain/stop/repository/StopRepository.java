package hufsbus.spring.domain.stop.repository;

import hufsbus.spring.domain.stop.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StopRepository extends JpaRepository<Stop, Long> {

    List<Stop> findByRouteIdOrderBySequenceAsc(Long routeId);
}
