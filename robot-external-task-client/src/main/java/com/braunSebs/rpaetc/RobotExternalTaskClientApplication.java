package com.braunSebs.rpaetc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class RobotExternalTaskClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(RobotExternalTaskClientApplication.class, args);
	}

    public RestTemplate getRpaBridgeRestClient(RestTemplateBuilder builder){

        return builder.build();

    }

}