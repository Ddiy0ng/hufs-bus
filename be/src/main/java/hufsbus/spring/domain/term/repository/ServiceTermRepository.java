package hufsbus.spring.domain.term.repository;

import hufsbus.spring.domain.term.entity.ServiceTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceTermRepository extends JpaRepository<ServiceTerm,Long> {
    Optional<ServiceTerm> findTopByOrderByIdDesc();
}