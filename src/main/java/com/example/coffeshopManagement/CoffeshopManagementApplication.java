package com.example.coffeshopManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.example.model")
public class CoffeshopManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoffeshopManagementApplication.class, args);
	}

}
