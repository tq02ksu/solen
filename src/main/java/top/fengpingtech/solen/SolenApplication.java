package top.fengpingtech.solen;

import top.fengpingtech.solen.server.ServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ServerProperties.class)
public class SolenApplication {
	public static void main(String[] args) {
		SpringApplication.run(SolenApplication.class, args);
	}
}
