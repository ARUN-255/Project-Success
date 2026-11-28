package com.bronzera.mybus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import java.time.Clock;
import java.time.ZoneId;

@EnableScheduling
@SpringBootApplication
@EnableSpringDataWebSupport
public class MyBusApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyBusApplication.class, args);
	}

	@Bean
	public Clock clock(){
		return Clock.system(ZoneId.of("Asia/Kolkata"));
	}

}
