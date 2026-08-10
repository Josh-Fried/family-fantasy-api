package family.fantasy.api.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import family.fantasy.api.core.Group.GameType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing family fantasy leagues.
 * This provides endpoints to create new groups, join existing groups using an 
 * invite code, and retrieve all the groups a specific user is currently participating in.
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
            
        // System.out.println("Controller: " + gameType);
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
        List<UserGroup> userGroups = groupService.getUserGroups(userId);
        
        // Map the database entities to a clean DTO to prevent infinite loops and control output
        List<GroupResponseDTO> responseDTOS = userGroups.stream().map(ug -> 
            new GroupResponseDTO(
                ug.getId(),
                ug.getGroup().getId(),
                ug.getGroup().getName(),
                ug.getGroup().getInviteCode(),
                ug.getIsAdmin(),
                ug.getGroup().getGameType()
            )
        ).collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOS);
    }

    // Static DTO record class to cleanly structure the response for the frontend
    public record GroupResponseDTO(
        Long userGroupId,
        Long groupId,
        String groupName,
        String inviteCode,
        boolean isAdmin,
        GameType gameType
    ) {}
}