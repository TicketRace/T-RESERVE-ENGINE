package com.treserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TReserveApplication {

    public static void main(String[] args) {
        SpringApplication.run(TReserveApplication.class, args);
    }
}
