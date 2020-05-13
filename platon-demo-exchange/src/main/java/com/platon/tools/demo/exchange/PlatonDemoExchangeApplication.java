package com.platon.tools.demo.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PlatonDemoExchangeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatonDemoExchangeApplication.class, args);
    }
}
