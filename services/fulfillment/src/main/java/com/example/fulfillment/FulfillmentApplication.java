package com.example.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class FulfillmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(FulfillmentApplication.class, args);
	}

}
