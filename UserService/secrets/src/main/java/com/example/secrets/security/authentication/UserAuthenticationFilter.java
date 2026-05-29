package com.example.secrets.security.authentication;

import com.example.secrets.entity.User;
import com.example.secrets.repository.UserRepository;
import com.example.secrets.security.config.SecurityConfiguration;
import com.example.secrets.security.service.JwtTokenService;
import com.example.secrets.security.service.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;

@Component
public class UserAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = recoverToken(request);
        if (token != null) {
            String email = jwtTokenService.getSubjectFromToken(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            UserDetailsImpl userDetails = new UserDetailsImpl(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.replace("Bearer ", "");
        }
        return null;
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return Arrays.asList(SecurityConfiguration.ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED).contains(uri);
    }
}