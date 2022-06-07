package top.fengpingtech.solen.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.fengpingtech.solen.app.domain.ConnectionDomain;

@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionDomain, String> {
}
