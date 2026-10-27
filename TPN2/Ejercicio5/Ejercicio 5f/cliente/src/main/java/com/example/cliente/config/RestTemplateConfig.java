package com.example.cliente.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
                HttpSession sesion = atributos.getRequest().getSession(false);
                if (sesion != null && sesion.getAttribute("token") != null) {
                    request.getHeaders().setBearerAuth((String) sesion.getAttribute("token"));
                }
            }
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
