package family.fantasy.api.core;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.github.bucket4j.Bucket;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SignupRateLimiterService rateLimiterService;

    public AuthController(AuthService authService, SignupRateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;

    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
    // Grab the real IP address, even if behind a cloud load balancer
    String ipAddress = httpRequest.getHeader("X-Forwarded-For");
    if (ipAddress == null || ipAddress.isEmpty()) {
        ipAddress = httpRequest.getRemoteAddr(); // Fallback for local development
    } else {
        ipAddress = ipAddress.split(",")[0].trim(); // Cloud providers sometimes attach a list; the first one is the user
    }
    
    Bucket bucket = rateLimiterService.resolveBucket(ipAddress);

    // Try to consume 1 token. If the bucket is empty, block them!
    if (!bucket.tryConsume(1)) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body("Too many signups from this IP address. Please try again later.");
    }

        try {
            // Passes the username and password from React into our new register method
            User user = authService.register(request.username(), request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (RuntimeException e) {
            // Returns a 400 Bad Request if the username is already taken
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // Passes the username and password from React into our new login method
            User user = authService.login(request.username(), request.password());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            // Returns a 401 Unauthorized if the credentials don't match
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // Updated record to accept both fields sent from the frontend
    public record AuthRequest(String username, String password) {}
}