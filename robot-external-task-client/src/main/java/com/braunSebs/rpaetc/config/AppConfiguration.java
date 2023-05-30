package com.braunSebs.rpaetc.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * AppConfiguration is a Spring configuration class that defines beans used in the application.
 */
@Configuration
@EnableScheduling
public class AppConfiguration {

    /**
     * Creates a RestTemplate bean with the name "uiPathDirectoryRestClient".
     *
     * @param builder the RestTemplateBuilder used to create the RestTemplate instance
     * @return a RestTemplate bean configured with the provided builder
     */
    @Bean(name = "uiPathDirectoryRestClient")
    public RestTemplate getActiveDirectoryRestClient(RestTemplateBuilder builder){
        return builder.build();
    }
}
