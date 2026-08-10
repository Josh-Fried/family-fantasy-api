package family.fantasy.api.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest request) {
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