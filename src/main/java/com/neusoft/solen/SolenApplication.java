package com.neusoft.solen;

import com.neusoft.solen.server.ServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(ServerProperties.class)
public class SolenApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolenApplication.class, args);
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer dateFormatCustomer() {
		return builder -> builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	}
}
