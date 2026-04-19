package com.financegame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinanceGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceGameApplication.class, args);
    }
}
