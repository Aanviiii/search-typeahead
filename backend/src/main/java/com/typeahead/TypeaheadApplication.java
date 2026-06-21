package com.typeahead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// (1) Meta-annotation: combines @Configuration + @EnableAutoConfiguration + @ComponentScan
@SpringBootApplication
// (2) Enables @Scheduled annotations to work (needed for BatchWriteScheduler)
@EnableScheduling
public class TypeaheadApplication {

    // (3) Java entry point — JVM starts here
    public static void main(String[] args) {
        // (4) Boots the entire Spring context:
        // - Scans all @Component, @Service, @Repository, @Controller classes
        // - Creates beans, injects dependencies
        // - Starts embedded Tomcat on port 8080
        // - Runs CommandLineRunner beans (DataLoaderConfig)
        SpringApplication.run(TypeaheadApplication.class, args);
    }
}