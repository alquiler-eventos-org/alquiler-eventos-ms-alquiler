package com.alquilereventos.alquiler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.alquilereventos.common.entity")
@EnableJpaRepositories(basePackages = "com.alquilereventos.common.repository")
public class MsAlquilerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAlquilerApplication.class, args);
	}

}