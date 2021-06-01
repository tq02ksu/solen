package top.fengpingtech.solen.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = SolenServerProperties.PREFIX)
@Data
public class SolenServerProperties {
    static final String PREFIX = "solen.server";

    private Integer port = 7889;

    private Integer ioThreads = 2;

    private Integer workerThreads = Runtime.getRuntime().availableProcessors() * 2;
}