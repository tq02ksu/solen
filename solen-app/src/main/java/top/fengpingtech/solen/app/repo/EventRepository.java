package top.fengpingtech.solen.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import top.fengpingtech.solen.app.domain.EventDomain;

public interface EventRepository extends JpaRepository<EventDomain, Long>, JpaSpecificationExecutor<EventDomain> {
}
