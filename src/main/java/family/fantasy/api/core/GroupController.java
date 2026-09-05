package family.fantasy.api.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import family.fantasy.api.core.Group.GameType;

import java.util.List;

/**
 * REST controller for managing family fantasy leagues.
 * This provides endpoints to create new groups, join existing groups using an 
 * invite code, retrieve all the groups a specific user is currently participating in,
 * fetch group details by ID, and get group leaderboards.
 */
@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final AuthService authService;

    public GroupController(GroupService groupService, AuthService authService) {
        this.groupService = groupService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(
            @RequestParam String tokenUsername,
            @RequestParam String groupName,
            @RequestParam String gameType) {
            
        User user = authService.getAuthenticatedUser(tokenUsername);
        Group newGroup = groupService.createGroup(user, groupName, gameType);
        return ResponseEntity.status(HttpStatus.CREATED).body(newGroup);
    }

    @PostMapping("/join")
    public ResponseEntity<Group> joinGroup(
            @RequestParam String tokenUsername,
            @RequestParam String inviteCode) {
        
        User user = authService.getAuthenticatedUser(tokenUsername);
        Group joinedGroup = groupService.joinGroup(user, inviteCode);
        return ResponseEntity.ok(joinedGroup);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupResponseDTO>> getUserGroups(@PathVariable Long userId) {
        // Delegates mapping to GroupService so we can dynamically calculate member count, points, etc.
        List<GroupResponseDTO> responseDTOS = groupService.getUserGroupsDetailed(userId);
        return ResponseEntity.ok(responseDTOS);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> getGroupById(@PathVariable Long groupId) {
        Group group = groupService.getGroupById(groupId);
        if (group == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Added default fallback values (0) for the new dynamic fields since they 
        // are only needed on the Home view, not the Group Details view
        GroupResponseDTO responseDTO = new GroupResponseDTO(
            null,
            group.getId(),
            group.getName(),
            group.getInviteCode(),
            false,
            group.getGameType(),
            0, 
            0, 
            0, 
            0
        );

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/{groupId}/leaderboard")
    public ResponseEntity<List<LeaderboardDTO>> getLeaderboard(@PathVariable Long groupId) {
        List<LeaderboardDTO> leaderboard = groupService.getLeaderboard(groupId);
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Endpoint for a user to leave a group.
     */
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            @RequestParam String tokenUsername) {
            
        User user = authService.getAuthenticatedUser(tokenUsername);
        groupService.leaveGroup(groupId, user.getId());
        return ResponseEntity.noContent().build();
    }

    // Static DTO record class structured for the frontend Home Page
    public record GroupResponseDTO(
        Long userGroupId,
        Long groupId,
        String groupName,
        String inviteCode,
        boolean isAdmin,
        GameType gameType,
        int memberCount,
        int userRank,
        int userPoints,
        int streak
    ) {}

    // Static DTO record class for leaderboard data
    public record LeaderboardDTO(
        Long userId,
        String displayName,
        int score,
        int streak,
        boolean isAdmin
    ) {}
}