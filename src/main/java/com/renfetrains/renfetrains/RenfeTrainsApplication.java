package com.renfetrains.renfetrains;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RenfeTrainsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RenfeTrainsApplication.class, args);
    }

}
