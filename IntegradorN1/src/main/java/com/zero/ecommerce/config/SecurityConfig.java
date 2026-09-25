package com.zero.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.zero.ecommerce.security.LoginAuthenticationFailureHandler;
import com.zero.ecommerce.security.LoginAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            LoginAuthenticationSuccessHandler successHandler,
            LoginAuthenticationFailureHandler failureHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/admin/usuarios", "/admin/usuarios/**",
                            "/admin/configuracion", "/admin/configuracion/**").hasRole("JEFE")
                    .requestMatchers("/admin", "/admin/**").hasAnyRole("JEFE", "ADMINISTRATIVO")
                    .requestMatchers("/cliente/**").hasRole("CLIENTE")
                    .anyRequest().permitAll())
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
                    .permitAll())
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())
            .exceptionHandling(exceptions -> exceptions
                    .accessDeniedHandler((request, response, exception) -> response.sendError(403)));
        return http.build();
    }
}
