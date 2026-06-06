package com.example.campuscrush;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampuscrushApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampuscrushApplication.class, args);
	}

}
