package com.dulcemayor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.dulcemayor.exception.ErroresSeguridadHandler;

/**
 * Configuración de seguridad PROVISORIA (E0-01): API sin estado, sin CSRF ni formularios de login,
 * y todas las rutas abiertas para que el equipo pueda probar la API antes de que exista el login.
 *
 * E1-01 reemplaza la regla "anyRequest().permitAll()" por el filtro JWT y las reglas por rol.
 * Mantener: STATELESS, CSRF deshabilitado, ErroresSeguridadHandler y /error abierto.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** E1-01: las claves se guardan con BCrypt (igual que IntegradorN1); nunca en texto plano. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ErroresSeguridadHandler errores)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/error").permitAll()
                        // TODO E1-01: reglas por rol (Jefe, Administrativo, Preventor, Distribuidor, Minorista)
                        .anyRequest().permitAll())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores));
        return http.build();
    }
}
