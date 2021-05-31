package top.fengpingtech.solen.server.config;

import lombok.Data;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.IdGenerator;

import java.util.List;

@Data
public class ServerProperties {

    private Integer port;

    private Integer readTimeout;

    private Integer ioThreads;

    private Integer workerThreads;

    private EventProcessor eventProcessor;

    private IdGenerator eventIdGenerator;
}
