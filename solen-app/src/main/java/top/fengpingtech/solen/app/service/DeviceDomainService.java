package top.fengpingtech.solen.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import top.fengpingtech.solen.app.domain.DeviceDomain;
import top.fengpingtech.solen.app.repository.ConnectionRepository;
import top.fengpingtech.solen.app.repository.DeviceRepository;
import top.fengpingtech.solen.app.repository.EventRepository;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class DeviceDomainService {
    private final DeviceRepository deviceRepository;

    private final EventRepository eventRepository;

    private final ConnectionRepository connectionRepository;

    public DeviceDomainService(DeviceRepository deviceRepository, EventRepository eventRepository, ConnectionRepository connectionRepository) {
        this.deviceRepository = deviceRepository;
        this.eventRepository = eventRepository;
        this.connectionRepository = connectionRepository;
    }

    public void delete(DeviceDomain device) {
        eventRepository.deleteByDevice(device);

        connectionRepository.deleteByDevice(device);

        deviceRepository.delete(device);
    }

    public Page<DeviceDomain> findAll(Specification<DeviceDomain> spec, Pageable page) {
        return deviceRepository.findAll(spec, page);
    }

    public Optional<DeviceDomain> findById(String deviceId) {
        return deviceRepository.findById(deviceId);
    }
}
