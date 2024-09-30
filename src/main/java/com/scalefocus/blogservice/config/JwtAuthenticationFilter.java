package com.scalefocus.blogservice.config;

import com.scalefocus.blogservice.dto.UserDetailsDto;
import com.scalefocus.blogservice.service.implementation.AuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthClient authClient;

    public JwtAuthenticationFilter(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        if (shouldSkipFilter(request, requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (authClient.validateToken(token)) {
                UserDetailsDto userDetails = authClient.getUserDetails(token);
                logger.info("User: " + userDetails.getUsername());
                logger.info("Roles: [" + userDetails.getRoles() + "]");

                List<GrantedAuthority> authorities = userDetails.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toUnmodifiableList());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean shouldSkipFilter(HttpServletRequest request, String requestURI) {
        return requestURI.startsWith("/h2-console") ||
                (request.getMethod().equals("GET") && requestURI.startsWith("/api/") && !requestURI.contains("admin")) ||
                "/api/blogposts/login".equals(requestURI);
    }

}

