package top.fengpingtech.solen.app.service;

import org.springframework.stereotype.Component;
import top.fengpingtech.solen.app.config.SolenServerProperties;
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

    private SolenServer server;

    public SolenServerStarter(SolenServerProperties serverProperties, EventProcessor eventProcessor) {
        this.serverProperties = serverProperties;
        this.eventProcessor = eventProcessor;
    }

    @PostConstruct
    public void init() {
        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(this.serverProperties.getPort());
        serverProperties.setIoThreads(this.serverProperties.getIoThreads());
        serverProperties.setWorkerThreads(this.serverProperties.getWorkerThreads());
        serverProperties.setEventProcessor(eventProcessor);
        serverProperties.setEventIdGenerator(new AtomicLong(0)::getAndIncrement);
        server = new SolenNettyServer(serverProperties);
        server.start();
    }

    public DeviceService getDeviceService() {
        return server.getDeviceService();
    }
}
