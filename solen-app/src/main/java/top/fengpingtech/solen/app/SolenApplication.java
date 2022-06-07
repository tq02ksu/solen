package top.fengpingtech.solen.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.fengpingtech.solen.app.config.AuthProperties;
import top.fengpingtech.solen.app.config.SolenServerProperties;

@SpringBootApplication
@EnableConfigurationProperties({AuthProperties.class, SolenServerProperties.class})
public class SolenApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolenApplication.class, args);
    }
}