package com.thecodereveal.shopease.auth.config;

import com.thecodereveal.shopease.auth.exceptions.RESTAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JWTTokenHelper jwtTokenHelper;

    @org.springframework.beans.factory.annotation.Value("${app.oauth.enabled:false}")
    private boolean oauthEnabled;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize)-> authorize
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/products/**","/api/category/**","/uploads/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/products/**","/api/category/**","/api/file/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT,"/api/products/**","/api/category/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/products/**","/api/category/**").hasAuthority("ADMIN")
                        .requestMatchers("/oauth2/success").permitAll()
                .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new RESTAuthenticationEntryPoint()))
                .addFilterBefore(new JWTAuthenticationFilter(jwtTokenHelper,userDetailsService), UsernamePasswordAuthenticationFilter.class);
        if (oauthEnabled) {
            http.oauth2Login(o -> o.defaultSuccessUrl("/oauth2/success").loginPage("/oauth2/authorization/google"));
        }
        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(){
        DaoAuthenticationProvider daoAuthenticationProvider= new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(daoAuthenticationProvider);

    }


    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
