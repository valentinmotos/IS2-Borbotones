package com.dulcemayor.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.dulcemayor.exception.ErroresSeguridadHandler;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtFiltro;
import com.dulcemayor.security.JwtService;

import jakarta.servlet.DispatcherType;

/**
 * Seguridad de la API (E1-01): JWT sin estado, claves con BCrypt y reglas por rol en el servidor.
 *
 * Matriz de acceso (la primera regla que coincide gana; cada issue suma las suyas ANTES de la
 * regla general de /api/**):
 * <pre>
 * POST /api/auth/login, /api/publico/**   abiertos
 * GET  /api/imagenes/{id}                 abierto (E1-05)
 * POST /api/imagenes                      JEFE, ADMINISTRATIVO (E1-05)
 * /api/auth/**                            cualquier usuario autenticado (también con clave temporal)
 * /api/categorias/**                      JEFE, ADMINISTRATIVO
 * POST /api/puntos-de-venta               PREVENTOR (E2-06)
 * GET  /api/puntos-de-venta/**            JEFE, ADMINISTRATIVO, PREVENTOR (E2-06)
 * /api/minorista/**                       MINORISTA
 * GET departamentos, localidades, geocod.  personal interno y MINORISTA (E2-07)
 * /api/**                                 personal interno: JEFE, ADMINISTRATIVO, PREVENTOR, DISTRIBUIDOR
 * cualquier otra ruta                     denegada
 * </pre>
 * El minorista recibe 403 en todo endpoint interno sin que haga falta marcar cada uno.
 * El JEFE tiene además los permisos de ADMINISTRATIVO y PREVENTOR (jerarquía de roles), que
 * también aplica a @PreAuthorize.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PERSONAL_INTERNO = { "JEFE", "ADMINISTRATIVO", "PREVENTOR", "DISTRIBUIDOR" };
    private static final List<String> ORIGENES_POR_DEFECTO = List.of("http://localhost:5173", "http://localhost:5174");

    /** Las claves se guardan con BCrypt (igual que IntegradorN1); nunca en texto plano. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** El Jefe puede todo lo del Administrativo y lo del Preventor. */
    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("JEFE").implies("ADMINISTRATIVO", "PREVENTOR")
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ErroresSeguridadHandler errores,
            JwtService jwtService, UsuarioRepository usuarioRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        // Públicos
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/publico/**").permitAll()
                        // E1-05: el sitio público muestra fotos de productos y sucursales
                        .requestMatchers(HttpMethod.GET, "/api/imagenes/*").permitAll()
                        // Sesión propia: cambio de clave, datos de la sesión y logout
                        .requestMatchers("/api/auth/**").authenticated()
                        // Reglas por recurso (cada issue agrega las suyas acá)
                        .requestMatchers("/api/categorias/**", "/api/productos/**", "/api/promociones/**")
                        .hasAnyRole("JEFE", "ADMINISTRATIVO")
                        // E2-02: stock y movimientos de almacén
                        .requestMatchers("/api/stock/**").hasAnyRole("JEFE", "ADMINISTRATIVO")
                        .requestMatchers("/api/preventa/**").hasRole("PREVENTOR")
                        // E2-06: el preventor da de alta comercios; gestión y preventa los consultan
                        .requestMatchers(HttpMethod.POST, "/api/puntos-de-venta").hasRole("PREVENTOR")
                        .requestMatchers(HttpMethod.GET, "/api/puntos-de-venta", "/api/puntos-de-venta/**")
                        .hasAnyRole("JEFE", "ADMINISTRATIVO", "PREVENTOR")
                        .requestMatchers("/api/puntos-de-venta/**").denyAll()
                        .requestMatchers("/api/empresas/**", "/api/sucursales/**", "/api/formas-de-pago/**")
                        .hasAnyRole("JEFE", "ADMINISTRATIVO")
                        .requestMatchers("/api/usuarios/internos/**").hasRole("JEFE")
                        // E1-05: suben imágenes quienes cargan productos, sucursales y la empresa
                        .requestMatchers(HttpMethod.POST, "/api/imagenes").hasAnyRole("JEFE", "ADMINISTRATIVO")
                        .requestMatchers("/api/minorista/**").hasRole("MINORISTA")
                        // E2-07: el minorista usa el componente de dirección de E1-04 en su perfil
                        .requestMatchers(HttpMethod.GET, "/api/departamentos", "/api/departamentos/**",
                                "/api/localidades/**", "/api/geocodificacion")
                        .hasAnyRole("JEFE", "ADMINISTRATIVO", "PREVENTOR", "DISTRIBUIDOR", "MINORISTA")
                        // Por defecto, todo /api es interno
                        .requestMatchers("/api/**").hasAnyRole(PERSONAL_INTERNO)
                        .anyRequest().denyAll())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores))
                .addFilterBefore(new JwtFiltro(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS solo para los dos clientes React (CORS_ORIGINS, separados por coma). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origenes:}") String origenes) {
        List<String> permitidos = origenes == null || origenes.isBlank()
                ? ORIGENES_POR_DEFECTO
                : Arrays.stream(origenes.split(",")).map(String::strip).filter(o -> !o.isEmpty()).toList();
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(permitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setExposedHeaders(List.of("Content-Disposition"));
        configuracion.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
