package com.baskaaleksander.nuvine.infrastrucure.config;


import com.baskaaleksander.nuvine.infrastrucure.security.RestAccessDeniedHandler;
import com.baskaaleksander.nuvine.infrastrucure.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAccessDeniedHandler accessDeniedHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/test").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    // todo fix that
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("realm_access.roles");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}