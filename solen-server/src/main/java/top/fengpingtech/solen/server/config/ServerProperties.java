package top.fengpingtech.solen.server.config;

import lombok.Data;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.IdGenerator;

@Data
public class ServerProperties {

    private int port = 7889;

    private int ioThreads = 2;

    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    private Boolean daemon;

    private EventProcessor eventProcessor;

    private IdGenerator eventIdGenerator;
}
