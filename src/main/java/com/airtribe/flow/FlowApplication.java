package com.airtribe.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.airtribe.flow")
@EnableJpaRepositories(basePackages = "com.airtribe.flow.repository")
@EntityScan(basePackages = "com.airtribe.flow.model")
public class FlowApplication {

    public static void main(String[] args) {
        // Force JVM timezone to UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FlowApplication.class, args);
    }
}
