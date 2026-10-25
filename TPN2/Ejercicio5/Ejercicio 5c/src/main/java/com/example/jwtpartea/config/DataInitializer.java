package com.example.jwtpartea.config;

import com.example.jwtpartea.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        userService.createUserIfMissing("admin", "123456", "ROLE_ADMIN");
    }
}
