package hufsbus.spring.domain.bus.repository;

import hufsbus.spring.domain.bus.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByTimetableId(Long timetableId);
    List<Bus> findByTimetableIdIn(List<Long> timetableIds);
}