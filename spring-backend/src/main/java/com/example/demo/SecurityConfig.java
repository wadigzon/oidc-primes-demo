package com.example.demo;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import io.jsonwebtoken.security.Keys;

@Configuration
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        //.cors().and()
        .csrf(csrf -> csrf.disable()) // disable CSRF for simplicity in dev
        .authorizeHttpRequests(authz -> authz
        .requestMatchers("/auth/**").permitAll() // ✅ this will include /auth/github, /auth/signup, /auth/signin 
        //.requestMatchers("/auth/github").permitAll() // 👈 specifically match the GitHub handler
            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // 👈 allow preflight
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .decoder(jwtDecoderResolver())
            )
        );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoderResolver() {
        // Google token decoder
        NimbusJwtDecoder googleDecoder = JwtDecoders.fromIssuerLocation("https://accounts.google.com");

        // Our custom token decoder
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        NimbusJwtDecoder customDecoder = NimbusJwtDecoder.withSecretKey(key).build();

        return token -> {
            try {
                return googleDecoder.decode(token);
            } catch (Exception e) {
                return customDecoder.decode(token);
            }
        };
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }    
}
