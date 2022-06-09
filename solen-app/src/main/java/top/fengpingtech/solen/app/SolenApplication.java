package top.fengpingtech.solen.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.fengpingtech.solen.app.config.AuthProperties;
import top.fengpingtech.solen.app.config.SolenServerProperties;
import top.fengpingtech.solen.app.service.SolenServerStarter;
import top.fengpingtech.solen.server.DeviceService;

@SpringBootApplication
@EnableConfigurationProperties({AuthProperties.class, SolenServerProperties.class})
public class SolenApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolenApplication.class, args);
    }

    @Bean
    DeviceService deviceService(SolenServerStarter starter) {
        return starter.getDeviceService();
    }
}