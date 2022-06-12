package top.fengpingtech.solen.app.service;

import org.springframework.stereotype.Component;
import top.fengpingtech.solen.app.config.SolenServerProperties;
import top.fengpingtech.solen.app.repository.EventRepository;
import top.fengpingtech.solen.server.DeviceService;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.SolenServer;
import top.fengpingtech.solen.server.config.ServerProperties;
import top.fengpingtech.solen.server.netty.SolenNettyServer;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SolenServerStarter {
    private final SolenServerProperties serverProperties;

    private final EventProcessor eventProcessor;

    private final EventRepository eventRepository;

    private SolenServer server;

    public SolenServerStarter(SolenServerProperties serverProperties, EventProcessor eventProcessor, EventRepository eventRepository) {
        this.serverProperties = serverProperties;
        this.eventProcessor = eventProcessor;
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    public void init() {
        Long maxId = eventRepository.getMaxId();

        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(this.serverProperties.getPort());
        serverProperties.setIoThreads(this.serverProperties.getIoThreads());
        serverProperties.setWorkerThreads(this.serverProperties.getWorkerThreads());
        serverProperties.setEventProcessor(eventProcessor);
        serverProperties.setEventIdGenerator(new AtomicLong(maxId == null ? 0 : maxId + 1)::getAndIncrement);
        server = new SolenNettyServer(serverProperties);
        server.start();
    }

    public DeviceService getDeviceService() {
        return server.getDeviceService();
    }
}
