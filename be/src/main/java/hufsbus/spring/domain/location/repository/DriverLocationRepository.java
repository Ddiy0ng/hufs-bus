package hufsbus.spring.domain.location.repository;

import hufsbus.spring.domain.location.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
    Optional<DriverLocation> findFirstByBusIdOrderByCreatedAtDesc(Long busId);
}
