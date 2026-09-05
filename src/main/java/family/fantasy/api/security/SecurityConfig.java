package family.fantasy.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;


import java.util.List;

/**
 * Global security configuration establishing the Security Filter Chain.
 * Handles CORS permissions, disables CSRF for stateless token APIs, 
 * and defines which endpoints require authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // @Bean
    // public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    //     http

        
    //         // 1. Configure CORS to use our custom source defined below
    //         .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
    //         // 2. Disable CSRF (Cross-Site Request Forgery) since we are using stateless JWT tokens, not session cookies
    //         .csrf(csrf -> csrf.disable())
            
    //         // 3. Ensure the server does not store user sessions (stateless)
    //         .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
    //         // 4. Define route permissions
    //         .authorizeHttpRequests(auth -> auth
    //             // Allow anyone to hit the login endpoint
    //             .requestMatchers("/api/v1/auth/login").permitAll()
    //             // Require authentication for every other endpoint
    //             .anyRequest().authenticated()
    //         )
    //         // Insert our custom JWT filter into the chain
    //         .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    //     return http.build();
    // }

    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http)
    //         throws Exception {
    //     http
    //             .csrf(AbstractHttpConfigurer::disable)
    //             .cors(withDefaults())
    //             .authorizeHttpRequests(auth -> auth
    //                     .requestMatchers("/api/v1/auth/**").permitAll()
    //                     .anyRequest().authenticated())
    //             .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    //             // The filter is now passed directly to the method where it's used
    //             .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    //     return http.build();
    // }

    // Updated bean inside SecurityConfig.java
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Lock down all admin routes to only users with the global admin authority
                .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_GLOBAL_ADMIN")
                // Require standard authentication for everything else
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Re-enable the JWT filter so tokens are actually checked
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    /**
     * Defines the specific CORS policy for the backend.
     * This determines which origins (frontend URLs), methods, and headers are allowed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins: Add your frontend URLs here (e.g., localhost for dev, actual domain for production)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000", // Common React/Next.js default
            "http://localhost:5173"  // Common Vite default
            // You will add your deployed frontend URL here later
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers (including Authorization for our JWT token)
        configuration.setAllowedHeaders(List.of("*"));
        
        // Allow credentials (important if you ever switch to cookies, though not strictly needed for basic Authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache the preflight request response for 1 hour to improve frontend performance
        configuration.setMaxAge(3600L);

        // Apply this configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;

    }
}