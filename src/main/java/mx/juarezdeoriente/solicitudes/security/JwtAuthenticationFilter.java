package mx.juarezdeoriente.solicitudes.security;
import mx.juarezdeoriente.solicitudes.security.AppUserDetailsService;
import mx.juarezdeoriente.solicitudes.security.JwtService;
import mx.juarezdeoriente.solicitudes.security.AppUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService            jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AppUserDetailsService userDetailsService) {
        this.jwtService         = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token    = authHeader.substring(7);
        String username = null;

        try {
            if (jwtService.isValid(token) && !jwtService.isRefreshToken(token)) {
                username = jwtService.extractUsername(token);
            }
        } catch (Exception ex) {
            log.debug("Token inválido: {}", ex.getMessage());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            AppUserDetails userDetails =
                    (AppUserDetails) userDetailsService.loadUserByUsername(username);

            int claimedVersion = jwtService.extractTokenVersion(token);
            if (claimedVersion != userDetails.getTokenVersion()) {
                log.debug("Token rechazado por tokenVersion obsoleta para usuario: {}", username);
                chain.doFilter(request, response);
                return;
            }

            if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
