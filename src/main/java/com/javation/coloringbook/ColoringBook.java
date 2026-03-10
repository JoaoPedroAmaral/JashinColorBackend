package com.javation.coloringbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ColoringBook {
    public static void main(String[] args) {
        SpringApplication.run(ColoringBook.class, args);
    }
}
