package family.fantasy.api.security;

import family.fantasy.api.core.AuthService;
import family.fantasy.api.core.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Custom filter that intercepts every HTTP request to check for a valid JWT token.
 * Extends OncePerRequestFilter to guarantee it executes only once per dispatch.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public JwtAuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. If there is no header or it doesn't start with "Bearer ", pass to the next filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the actual token string (skipping "Bearer ")
        final String token = authHeader.substring(7);

        try {
            // 4. Validate the token and retrieve the user from our database using AuthService
            User user = authService.getAuthenticatedUser(token);

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 5. Create the Spring Security authentication object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        new ArrayList<>() 
                );

                // Add request details to the authentication token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Inject the authenticated user into the Spring Security Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            System.out.println("Failed to authenticate token: " + e.getMessage());
        }

        // 7. Continue the filter chain
        filterChain.doFilter(request, response);
    }
}