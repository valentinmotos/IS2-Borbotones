package com.example.mascotas.seguridad;

import com.example.mascotas.servicios.UsuarioServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.server.ResponseStatusException;

@Configuration
public class SeguridadConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.oauth.issuer}") String issuer,
            @Value("${app.oauth.jwk-set-uri}") String jwks, @Value("${app.oauth.audience}") String audience) {
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> destino = token -> token.getAudience().contains(audience)
                && token.getSubject()!=null && token.getExpiresAt()!=null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Token sin audiencia, sub o vencimiento valido",null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),destino));
        return decoder;
    }
    @Bean
    SecurityFilterChain api(HttpSecurity http, UsuarioServicio usuarios) throws Exception {
        var scopes=new JwtGrantedAuthoritiesConverter();
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable()).formLogin(f -> f.disable()).httpBasic(b -> b.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET,"/api/**").hasAuthority("SCOPE_api.read")
                        .requestMatchers(HttpMethod.POST,"/api/**").hasAuthority("SCOPE_api.write")
                        .requestMatchers(HttpMethod.PUT,"/api/**").hasAuthority("SCOPE_api.write")
                        .requestMatchers(HttpMethod.DELETE,"/api/**").hasAuthority("SCOPE_api.write")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                    try {
                        String id=usuarios.identificar(jwt).getId();
                        return new JwtAuthenticationToken(jwt,scopes.convert(jwt),id);
                    } catch (ResponseStatusException ex) { throw new InvalidBearerTokenException("Usuario no disponible"); }
                })))
                .build();
    }
}
