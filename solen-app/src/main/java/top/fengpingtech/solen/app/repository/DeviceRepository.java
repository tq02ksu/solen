package top.fengpingtech.solen.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import top.fengpingtech.solen.app.domain.DeviceDomain;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceDomain, String>, JpaSpecificationExecutor<DeviceDomain> {
}
