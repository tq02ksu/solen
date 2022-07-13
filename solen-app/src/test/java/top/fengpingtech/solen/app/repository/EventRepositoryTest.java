package top.fengpingtech.solen.app.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import top.fengpingtech.solen.app.SolenApplicationTests;
import top.fengpingtech.solen.app.domain.DeviceDomain;
import top.fengpingtech.solen.app.domain.EventDomain;
import top.fengpingtech.solen.server.model.EventType;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class EventRepositoryTest extends SolenApplicationTests {
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeAll
    void prepare() {
        DeviceDomain device = DeviceDomain.builder().deviceId("42321011102").build();
        deviceRepository.save(device);

        EventDomain event1 = EventDomain.builder()
                .device(device)
                .type(EventType.ATTRIBUTE_UPDATE)
                .time(new Date())
                .build();

        EventDomain event2 = EventDomain.builder()
                .device(device)
                .type(EventType.ATTRIBUTE_UPDATE)
                .time(new Date())
                .build();

        EventDomain event3 = EventDomain.builder()
                .device(device)
                .type(EventType.ATTRIBUTE_UPDATE)
                .time(new Date())
                .build();

        eventRepository.save(event1);
        eventRepository.save(event2);
        eventRepository.save(event3);
    }

    @Test
    void deleteAll() {
        DeviceDomain device = deviceRepository.getById("42321011102");
//        eventRepository.findBy(device);
    }
}