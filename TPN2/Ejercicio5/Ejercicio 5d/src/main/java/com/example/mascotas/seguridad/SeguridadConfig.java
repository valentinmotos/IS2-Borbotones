package com.example.mascotas.seguridad;

import com.example.mascotas.repositorios.UsuarioRepositorio;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SeguridadConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtServicio jwt, UsuarioRepositorio usuarios) throws Exception {
        return http
                // La API solo autentica con Authorization Bearer; no usa cookies de autenticacion.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(l -> l.disable())
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.js", "/styles.css", "/favicon.ico", "/api/zonas").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .requestMatchers("/api/**").hasRole("USUARIO")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.setHeader("WWW-Authenticate", "Bearer");
                    res.getWriter().write("{\"mensaje\":\"Debe iniciar sesion con un JWT valido\"}");
                }).accessDeniedHandler((req,res,ex) -> {
                    res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"mensaje\":\"No tiene permisos para esta operacion\"}");
                }))
                .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' blob:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")))
                .addFilterBefore(new JwtFiltro(jwt, usuarios), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
