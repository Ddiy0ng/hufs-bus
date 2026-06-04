package hufsbus.spring.domain.term.repository;

import hufsbus.spring.domain.term.entity.PrivacyTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivacyTermRepository extends JpaRepository<PrivacyTerm, Long> {
    Optional<PrivacyTerm> findTopByOrderByIdDesc();
}
