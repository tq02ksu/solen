package top.fengpingtech.solen.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.fengpingtech.solen.app.domain.ConnectionDomain;
import top.fengpingtech.solen.app.domain.DeviceDomain;

import java.util.List;

@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionDomain, String> {
    List<ConnectionDomain> findByDevice(DeviceDomain deviceDomain);

    void deleteByDevice(DeviceDomain device);
}
