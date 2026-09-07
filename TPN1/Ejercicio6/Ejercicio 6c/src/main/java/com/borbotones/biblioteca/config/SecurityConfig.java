package com.borbotones.biblioteca.config;

import com.borbotones.biblioteca.model.Usuario;
import com.borbotones.biblioteca.repo.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return mail -> usuarios.findByMail(mail).filter(Usuario::isAlta)
            .map(u -> User.withUsername(u.getMail()).password(u.getClave())
                .roles(u.getRol().name()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/registro", "/css/**", "/error").permitAll()
            .requestMatchers("/admin/**", "/autores/**", "/editoriales/**").hasRole("ADMIN")
            .anyRequest().authenticated())
            .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}