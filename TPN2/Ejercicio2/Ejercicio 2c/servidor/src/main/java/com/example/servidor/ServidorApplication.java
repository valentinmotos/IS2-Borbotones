package com.example.servidor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling //Habilita las tareas programadas, IMPORTANTE
public class ServidorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServidorApplication.class, args);
    }
}
