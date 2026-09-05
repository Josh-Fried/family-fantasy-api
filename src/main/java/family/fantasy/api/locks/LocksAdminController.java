package family.fantasy.api.locks;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller dedicated to group-level administrative actions for the Locks feature.
 */
@RestController
@RequestMapping("/api/v1/admin/locks")
public class LocksAdminController {

    private final LocksAdminService locksAdminService;

    /**
     * Constructs a new LocksAdminController with the underlying locks admin service layer.
     *
     * @param locksAdminService the service encapsulating all group-level administrative logic
     */
    public LocksAdminController(LocksAdminService locksAdminService) {
        this.locksAdminService = locksAdminService;
    }

    /**
     * Removes a specific member from the group, provided the requester is a group admin.
     *
     * @param groupId       the unique identifier of the group
     * @param memberId      the unique identifier of the member to be removed
     * @param tokenUsername the authentication token or username of the requester
     * @return a no-content response entity indicating successful removal
     */
    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @RequestParam String tokenUsername) {
            
        locksAdminService.removeMember(groupId, memberId, tokenUsername);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes the entire group and its associated data, provided the requester is a group admin.
     *
     * @param groupId       the unique identifier of the group to delete
     * @param tokenUsername the authentication token or username of the requester
     * @return a no-content response entity indicating successful deletion
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @RequestParam String tokenUsername) {
            
        locksAdminService.deleteGroup(groupId, tokenUsername);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the basic details of the group, such as its display name.
     *
     * @param groupId       the unique identifier of the group to update
     * @param newName       the new name to assign to the group
     * @param tokenUsername the authentication token or username of the requester
     * @return a response entity containing a success message
     */
    @PutMapping("/{groupId}/name")
    public ResponseEntity<?> updateGroupName(
            @PathVariable Long groupId,
            @RequestParam String newName,
            @RequestParam String tokenUsername) {
            
        locksAdminService.updateGroupName(groupId, newName, tokenUsername);
        return ResponseEntity.ok(Map.of("message", "Group name successfully updated to " + newName));
    }

    /**
     * Regenerates the invitation code for the group, invalidating any previous codes.
     *
     * @param groupId       the unique identifier of the group
     * @param tokenUsername the authentication token or username of the requester
     * @return a response entity containing the newly generated invite code
     */
    @PostMapping("/{groupId}/invite-code/regenerate")
    public ResponseEntity<?> regenerateInviteCode(
            @PathVariable Long groupId,
            @RequestParam String tokenUsername) {
            
        String newCode = locksAdminService.regenerateInviteCode(groupId, tokenUsername);
        return ResponseEntity.ok(Map.of(
                "message", "Invite code successfully regenerated.",
                "inviteCode", newCode
        ));
    }

    /**
     * Transfers administrative ownership of the group to another existing member.
     *
     * @param groupId       the unique identifier of the group
     * @param newAdminId    the unique identifier of the member receiving ownership
     * @param tokenUsername the authentication token or username of the current owner
     * @return a response entity confirming the transfer of ownership
     */
    @PutMapping("/{groupId}/transfer-ownership/{newAdminId}")
    public ResponseEntity<?> transferOwnership(
            @PathVariable Long groupId,
            @PathVariable Long newAdminId,
            @RequestParam String tokenUsername) {
            
        locksAdminService.transferOwnership(groupId, newAdminId, tokenUsername);
        return ResponseEntity.ok(Map.of("message", "Group ownership successfully transferred."));
    }

    /**
     * Allows a group administrator to manually override a selected team for an existing pick entry within their group.
     *
     * @param groupId       the unique identifier of the group where the pick resides
     * @param pickId        the unique identifier of the pick database record
     * @param correctedTeam the new team string to assign to the user's pick
     * @param tokenUsername the authentication token or username of the group administrator
     * @return a response entity delivering the updated pick record
     */
    @PutMapping("/{groupId}/picks/{pickId}/override-team")
    public ResponseEntity<Pick> overridePickSelection(
            @PathVariable Long groupId,
            @PathVariable Long pickId,
            @RequestParam String correctedTeam,
            @RequestParam String tokenUsername) {
            
        Pick updatedPick = locksAdminService.overridePickSelection(groupId, pickId, correctedTeam, tokenUsername);
        return ResponseEntity.ok(updatedPick);
    }
}