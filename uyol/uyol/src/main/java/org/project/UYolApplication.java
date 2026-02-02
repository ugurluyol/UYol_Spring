package org.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.project")
public class UYolApplication {
    public static void main(String[] args) {
        SpringApplication.run(UYolApplication.class, args);
    }
}

