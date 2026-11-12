package com.dulcemayor.config;

import java.net.URI;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.util.HttpSessionMutexListener;

import com.dulcemayor.exception.ErroresSeguridadHandler;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.GoogleOAuthFlow;
import com.dulcemayor.security.JwtFiltro;
import com.dulcemayor.security.JwtService;

import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@ConditionalOnProperty(name = "app.oauth.google.enabled", havingValue = "true")
public class GoogleOAuthConfig {

    private final URI portal;
    private final ClientRegistration google;

    public GoogleOAuthConfig(@Value("${app.oauth.google.client-id}") String clientId,
            @Value("${app.oauth.google.client-secret}") String clientSecret,
            @Value("${app.oauth.google.public-url}") URI portal) {
        boolean local = "localhost".equalsIgnoreCase(portal.getHost()) || "127.0.0.1".equals(portal.getHost());
        if (portal.getHost() == null || portal.getUserInfo() != null || portal.getQuery() != null
                || portal.getFragment() != null || !(portal.getPath().isEmpty() || "/".equals(portal.getPath()))
                || !("https".equalsIgnoreCase(portal.getScheme()) || local && "http".equalsIgnoreCase(portal.getScheme()))
                || clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "Google OAuth requiere client ID, client secret y el origen HTTPS del portal (HTTP solo local).");
        }
        this.portal = portal;
        this.google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .issuerUri(GoogleOAuthFlow.EMISOR)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "email")
                .redirectUri(portal.resolve(GoogleOAuthFlow.CALLBACK).toString())
                .build();
    }

    @Bean
    ServletContextInitializer googleSessionCookie() {
        return context -> {
            context.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
            context.getSessionCookieConfig().setName("DULCEMAYOR_OAUTH");
            context.getSessionCookieConfig().setHttpOnly(true);
            context.getSessionCookieConfig().setSecure("https".equalsIgnoreCase(portal.getScheme()));
            context.setSessionTimeout(10);
        };
    }

    @Bean
    ServletListenerRegistrationBean<HttpSessionMutexListener> googleSessionMutex() {
        return new ServletListenerRegistrationBean<>(new HttpSessionMutexListener());
    }

    @Bean
    ClientRegistrationRepository googleClientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(google);
    }

    @Bean
    @Order(1)
    SecurityFilterChain googleOAuthChain(HttpSecurity http, JwtService jwtService, UsuarioRepository usuarios,
            ErroresSeguridadHandler errores, ClientRegistrationRepository clientes) throws Exception {
        String base = GoogleOAuthFlow.BASE;
        var autorizacion = new DefaultOAuth2AuthorizationRequestResolver(clientes, GoogleOAuthFlow.INICIAR);
        autorizacion.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        var oidc = new OidcUserService();
        oidc.setRetrieveUserInfo(solicitud -> false);
        http.securityMatcher(base + "/**")
                .securityContext(contexto -> contexto.securityContextRepository(new NullSecurityContextRepository()))
                // Solo el callback OIDC rota la cookie y el CSRF. Cada petición con JWT ya está autenticada.
                .sessionManagement(sesion -> sesion.requireExplicitAuthenticationStrategy(true))
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.GET, GoogleOAuthFlow.INICIAR + "/google",
                                GoogleOAuthFlow.CALLBACK, base + "/estado").permitAll()
                        .requestMatchers(HttpMethod.POST, base + "/sesion", base + "/registro").permitAll()
                        .requestMatchers(HttpMethod.POST, base + "/vincular", base + "/desvincular").hasRole("MINORISTA")
                        .anyRequest().denyAll())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(errores).accessDeniedHandler(errores))
                .addFilterBefore(new JwtFiltro(jwtService, usuarios), UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth -> oauth.clientRegistrationRepository(clientes)
                        .authorizedClientRepository(new ClientesSinAlmacenamiento())
                        .loginPage("/login")
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(autorizacion))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri(GoogleOAuthFlow.CALLBACK))
                        .userInfoEndpoint(info -> info.oidcUserService(oidc))
                        .successHandler((request, response, authentication) -> {
                            GoogleOAuthFlow.limpiar(request);
                            boolean valido = authentication.getPrincipal() instanceof OidcUser usuario
                                    && GoogleOAuthFlow.guardar(request, usuario);
                            volverAlPortal(response, valido ? "" : "?error=correo");
                        })
                        .failureHandler((request, response, exception) -> {
                            volverAlPortal(response, "?error=acceso");
                        }));
        return http.build();
    }

    private void volverAlPortal(HttpServletResponse response, String resultado) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader(HttpHeaders.LOCATION, portal.resolve("/acceso/google" + resultado).toString());
    }

    /** La API usa su propio JWT. Los tokens de Google solo se necesitan durante el callback. */
    private static final class ClientesSinAlmacenamiento implements OAuth2AuthorizedClientRepository {
        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id, Authentication autenticacion,
                HttpServletRequest request) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(OAuth2AuthorizedClient cliente, Authentication autenticacion,
                HttpServletRequest request, HttpServletResponse response) {
        }

        @Override
        public void removeAuthorizedClient(String id, Authentication autenticacion, HttpServletRequest request,
                HttpServletResponse response) {
        }
    }
}
