package com.borbotones.autorizacion;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import javax.sql.DataSource;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
public class SeguridadConfig {
    @Bean @Order(1)
    SecurityFilterChain protocolo(HttpSecurity http) throws Exception {
        var oauth=OAuth2AuthorizationServerConfigurer.authorizationServer();
        return http.securityMatcher(oauth.getEndpointsMatcher())
                .with(oauth, a -> a.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"), new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .build();
    }
    @Bean @Order(2)
    SecurityFilterChain login(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a.requestMatchers("/registro", "/error").permitAll().anyRequest().authenticated())
                .formLogin(Customizer.withDefaults()).build();
    }
    @Bean PasswordEncoder encoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean
    @org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
    JdbcUserDetailsManager usuarios(DataSource dataSource, PasswordEncoder encoder, @Value("${app.demo.password}") String password,
                               @Value("${app.demo.admin-password}") String adminPassword) {
        var manager=new JdbcUserDetailsManager(dataSource);
        if (!manager.userExists("usuario")) manager.createUser(User.withUsername("usuario").password(encoder.encode(password)).roles("USUARIO").build());
        if (!manager.userExists("admin")) manager.createUser(User.withUsername("admin").password(encoder.encode(adminPassword)).roles("USUARIO","ADMIN").build());
        return manager;
    }
    @Bean
    RegisteredClientRepository clientes(PasswordEncoder encoder, @Value("${app.oauth.client-id}") String id,
            @Value("${app.oauth.client-secret}") String secreto, @Value("${app.oauth.client-url}") String url) {
        var cliente=RegisteredClient.withId(UUID.randomUUID().toString()).clientId(id)
                .clientSecret(encoder.encode(secreto))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(url+"/login/oauth2/code/local").postLogoutRedirectUri(url+"/")
                .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope("api.read").scope("api.write")
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofMinutes(30)).reuseRefreshTokens(false).build()).build();
        return new InMemoryRegisteredClientRepository(cliente);
    }
    @Bean
    JWKSource<SecurityContext> claves() throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        var pair=generator.generateKeyPair();
        var key=new RSAKey.Builder((RSAPublicKey)pair.getPublic()).privateKey((RSAPrivateKey)pair.getPrivate())
                .keyID(UUID.randomUUID().toString()).build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }
    @Bean JwtDecoder decoder(JWKSource<SecurityContext> claves) { return OAuth2AuthorizationServerConfiguration.jwtDecoder(claves); }
    @Bean AuthorizationServerSettings servidor(@Value("${app.oauth.issuer}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> claims(@Value("${app.oauth.audience}") String audience) {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().audience(List.of(audience));
                context.getClaims().claim("name", context.getPrincipal().getName());
                context.getClaims().claim("roles", context.getPrincipal().getAuthorities().stream()
                        .map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).toList());
            }
        };
    }
}
