package top.fengpingtech.solen.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.fengpingtech.solen.app.domain.Connection;

public interface ConnectionRepository extends JpaRepository<Connection, String> {
}
