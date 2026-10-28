package com.borbotones.cliente;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {
    @Bean
    ClientRegistrationRepository clientes(@Value("${app.oauth.issuer}") String issuer,
            @Value("${app.oauth.client-id}") String id, @Value("${app.oauth.client-secret}") String secret) {
        // Endpoints explicitos: no se requiere que el proveedor este disponible al arrancar el cliente.
        var local=ClientRegistration.withRegistrationId("local").clientName("Tinder de Mascotas")
                .clientId(id).clientSecret(secret).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid","profile","api.read","api.write").issuerUri(issuer)
                .authorizationUri(issuer+"/oauth2/authorize").tokenUri(issuer+"/oauth2/token")
                .jwkSetUri(issuer+"/oauth2/jwks").userInfoUri(issuer+"/userinfo").userNameAttributeName("sub")
                .providerConfigurationMetadata(Map.of("end_session_endpoint",issuer+"/connect/logout")).build();
        return new InMemoryClientRegistrationRepository(local);
    }
    @Bean OAuth2AuthorizedClientRepository autorizados() { return new HttpSessionOAuth2AuthorizedClientRepository(); }
    @Bean
    OAuth2AuthorizedClientManager manager(ClientRegistrationRepository clientes, OAuth2AuthorizedClientRepository autorizados) {
        var manager=new DefaultOAuth2AuthorizedClientManager(clientes,autorizados);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().refreshToken().build());
        return manager;
    }
    @Bean
    SecurityFilterChain web(HttpSecurity http, ClientRegistrationRepository clientes, OAuth2AuthorizedClientRepository autorizados) throws Exception {
        var resolver=new DefaultOAuth2AuthorizationRequestResolver(clientes,"/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        var logout=new OidcClientInitiatedLogoutSuccessHandler(clientes); logout.setPostLogoutRedirectUri("{baseUrl}/");
        return http.authorizeHttpRequests(a -> a
                        .requestMatchers("/","/index.html","/app.js","/styles.css","/favicon.ico","/registro","/error","/login/**","/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor((req,res,ex) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"mensaje\":\"Inicia sesion para continuar\"}");
                }, req -> req.getRequestURI().startsWith("/api/") || req.getRequestURI().equals("/sesion")))
                .oauth2Login(o -> o.authorizedClientRepository(autorizados)
                        .authorizationEndpoint(a -> a.authorizationRequestResolver(resolver))
                        .defaultSuccessUrl("/",true))
                .logout(l -> l.logoutSuccessHandler(logout).deleteCookies("E5_CLIENT_SESSION"))
                .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' blob:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")))
                .build();
    }
}
