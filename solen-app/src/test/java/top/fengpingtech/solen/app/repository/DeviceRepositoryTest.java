package top.fengpingtech.solen.app.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import top.fengpingtech.solen.app.SolenApplicationTests;
import top.fengpingtech.solen.app.domain.ConnectionStatus;
import top.fengpingtech.solen.app.domain.DeviceDomain;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRepositoryTest extends SolenApplicationTests {

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    public void save() {
        DeviceDomain domain = new DeviceDomain();
        domain.setDeviceId("4002001001");
        domain.setStatus(ConnectionStatus.NORMAL);
        deviceRepository.save(domain);
    }
}