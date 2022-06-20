package top.fengpingtech.solen.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import top.fengpingtech.solen.app.domain.DeviceDomain;
import top.fengpingtech.solen.app.domain.EventDomain;
import top.fengpingtech.solen.server.model.EventType;

import java.util.Date;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventDomain, Long>, JpaSpecificationExecutor<EventDomain> {
    void deleteByTimeLessThan(Date startTime);

    Page<EventDomain> findByDeviceAndType(DeviceDomain domain, EventType type, Pageable pageable);

    void deleteByDevice(DeviceDomain device);
}
