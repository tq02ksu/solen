package top.fengpingtech.solen.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.fengpingtech.solen.app.config.AuthProperties;
import top.fengpingtech.solen.app.config.SolenServerProperties;
import top.fengpingtech.solen.server.DeviceService;
import top.fengpingtech.solen.server.SolenServer;
import top.fengpingtech.solen.server.config.ServerProperties;
import top.fengpingtech.solen.server.netty.SolenNettyServer;

@SpringBootApplication
@EnableConfigurationProperties({AuthProperties.class, SolenServerProperties.class})
public class SolenApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolenApplication.class, args);
    }

    @Bean
    SolenServer server (SolenServerProperties props) {
        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(props.getPort());
        serverProperties.setIoThreads(props.getIoThreads());
        serverProperties.setWorkerThreads(props.getWorkerThreads());
        SolenServer server = new SolenNettyServer(serverProperties);
        server.start();
        return server;
    }

    @Bean
    DeviceService deviceService(SolenServer server) {
        return server.getDeviceService();
    }
}