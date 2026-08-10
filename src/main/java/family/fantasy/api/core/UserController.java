package family.fantasy.api.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing core user operations.
 * This exposes endpoints for token-based authentication, user registration, 
 * and updating profile details such as the user's display name across all games.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    // We use constructor injection to bring in the services
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // Endpoint to let a user change their display name
    @PutMapping("/{userId}/display-name")
    public ResponseEntity<User> updateDisplayName(
            @PathVariable Long userId,
            @RequestParam String newDisplayName) {
        
        User updatedUser = userService.updateDisplayName(userId, newDisplayName);
        return ResponseEntity.ok(updatedUser);
    }
}