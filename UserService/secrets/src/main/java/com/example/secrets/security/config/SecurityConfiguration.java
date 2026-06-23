package com.example.secrets.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.secrets.security.authentication.UserAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    // ENDPOINTS PÚBLICOS ========
    public static final String[] ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED = {
        "/users/login",
        "/users",
        "/users/auth/request-code"
    };

    // ENDPOINTS COM ACESSO RESTRITO ========
    public static final String[] ENDPOINTS_ADMIN = {
        "/users/test/administrator"
    };

    public static final String[] ENDPOINTS_CUSTOMER = {
        "/users/test/customer"
    };

    @Autowired
    private UserAuthenticationFilter userAuthenticationFilter;

   @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Desabilita CSRF (apropriado para APIs stateless)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Sem sessão no servidor
            .authorizeHttpRequests(authorize -> authorize
                // 1. Libera o array de endpoints que não exigem autenticação
                .requestMatchers(ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED).permitAll()
                
                // 2. Garante o acesso público às rotas com e sem o prefixo /users de forma correta
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/auth/request-code", "/auth/request-code").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/login", "/login").permitAll()
                
                // 3. Suas regras de perfis (Roles)
                .requestMatchers(ENDPOINTS_ADMIN).hasRole("ADMINISTRATOR")
                .requestMatchers(ENDPOINTS_CUSTOMER).hasRole("CUSTOMER")
                
                // Não deixa o Spring Security bloquear a requisição antes de chegar no controller
                .requestMatchers("/users/update-profile").permitAll()
                
                // Qualquer outra rota exige login
                .anyRequest().authenticated()
            )
            .addFilterBefore(userAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Codificador de senhas (BCrypt)
    }
}