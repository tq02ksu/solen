package top.fengpingtech.solen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.fengpingtech.solen.auth.AuthProperties;
import top.fengpingtech.solen.server.ServerProperties;
import top.fengpingtech.solen.service.RaftProperties;

@SpringBootApplication
@EnableConfigurationProperties({ServerProperties.class, AuthProperties.class, RaftProperties.class})
public class SolenApplication {
	public static void main(String[] args) {
		SpringApplication.run(SolenApplication.class, args);
	}
}
