package family.fantasy.api.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    private final GroupService groupService;

    // Constructor injection bringing in all required services
    public UserController(UserService userService, AuthService authService, GroupService groupService) {
        this.userService = userService;
        this.authService = authService;
        this.groupService = groupService;
    }

    // Endpoint to let a user change their display name
    @PutMapping("/{userId}/display-name")
    public ResponseEntity<User> updateDisplayName(
            @PathVariable Long userId,
            @RequestParam String newDisplayName) {
        
        User updatedUser = userService.updateDisplayName(userId, newDisplayName);
        return ResponseEntity.ok(updatedUser);
    }

    // Endpoint to fetch recent game results for a user
    @GetMapping("/{userId}/recent-results")
    public ResponseEntity<List<RecentResultDTO>> getRecentResults(
            @PathVariable Long userId, 
            @RequestParam(defaultValue = "3") int limit) {
        
        List<RecentResultDTO> results = groupService.getRecentUserResults(userId, limit);
        return ResponseEntity.ok(results);
    }

    public record RecentResultDTO(
        String gameType,
        int week,
        String groupName,
        String placementIcon,
        int place,
        String placeSuffix,
        int points
    ) {}
}