package ar.edu.clinica.config;

import ar.edu.clinica.model.Usuario;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Seguridad {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService cuentas(EntityManager em) {
        return nombre -> {
            Usuario u=em.createQuery("from Usuario u where u.nombreUsuario=:nombre and u.eliminado=false",Usuario.class)
                .setParameter("nombre",nombre).getResultList().stream().findFirst().orElseThrow(()->new UsernameNotFoundException("Credenciales inválidas"));
            return User.withUsername(u.nombreUsuario).password(u.clave).roles("CONSULTORIO").build();
        };
    }
    @Bean SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a->a.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // La API recibe Basic explícito; los clientes no utilizan cookies de autenticación.
            .csrf(c->c.disable()).build();
    }
}
