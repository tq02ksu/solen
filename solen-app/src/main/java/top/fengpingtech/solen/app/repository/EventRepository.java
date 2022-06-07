package top.fengpingtech.solen.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import top.fengpingtech.solen.app.domain.EventDomain;

@Repository
public interface EventRepository extends JpaRepository<EventDomain, Long>, JpaSpecificationExecutor<EventDomain> {
}
