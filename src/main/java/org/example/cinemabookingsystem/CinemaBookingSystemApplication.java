package org.example.cinemabookingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CinemaBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinemaBookingSystemApplication.class, args);
	}

}
