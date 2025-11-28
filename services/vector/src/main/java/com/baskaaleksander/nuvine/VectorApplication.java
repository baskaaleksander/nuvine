package com.baskaaleksander.nuvine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class VectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(VectorApplication.class, args);
    }

}
