package com.example.mascotas.configuracion;

import com.example.mascotas.entidades.Usuario;
import com.example.mascotas.repositorios.UsuarioRespositorio;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SeguridadConfiguracion {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/registro", "/registrar", "/css/**", "/img/**", "/vendor/**", "/foto/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                        .usernameParameter("email").passwordParameter("clave").successHandler(successHandler)
                        .failureUrl("/login?error").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler(UsuarioRespositorio usuarioRepositorio) {
        return (request, response, authentication) -> {
            Usuario usuario = usuarioRepositorio.buscarPorMail(authentication.getName());
            HttpSession session = request.getSession(true);
            session.setAttribute("usuariosession", usuario);
            response.sendRedirect(request.getContextPath() + "/inicio");
        };
    }
}
