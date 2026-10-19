package com.thecodereveal.shopease.config;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
@Profile("local")
public class LocalUploadsConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(java.nio.file.Path.of("uploads").toAbsolutePath().toUri().toString()+"/");
    }
}
