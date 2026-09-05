package family.fantasy.api.locks;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import family.fantasy.api.core.User;
import family.fantasy.api.core.AuthService;
import family.fantasy.api.core.GroupService;

/**
 * Service dedicated to managing group-level administrative actions for the Locks feature.
 */
@Service
public class LocksAdminService {

    private final GroupService groupService;
    private final AuthService authService;
    private final PickRepository pickRepository;

    /**
     * Constructs a new LocksAdminService with the required services and repositories.
     *
     * @param groupService   service for managing group-related actions and validations
     * @param authService    service for authenticating users via token credentials
     * @param pickRepository repository for accessing and modifying user lock picks
     */
    public LocksAdminService(GroupService groupService, AuthService authService, PickRepository pickRepository) {
        this.groupService = groupService;
        this.authService = authService;
        this.pickRepository = pickRepository;
    }

    /**
     * Authenticates the user from the token and verifies they have admin rights for the specified group.
     * Throws an unauthorized or forbidden exception if the user lacks the proper privileges.
     *
     * @param groupId       the unique identifier of the group being accessed
     * @param tokenUsername the authentication token or username of the requester
     * @return the authenticated User object if they are verified as a group admin
     */
    public User verifyGroupAdmin(Long groupId, String tokenUsername) {
        User requester = authService.getAuthenticatedUser(tokenUsername);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication failed.");
        }
        
        if (!groupService.isAdmin(groupId, requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Group administrative privileges required.");
        }
        
        return requester;
    }

    /**
     * Removes a specific member from the group, provided the requester is a verified group admin.
     *
     * @param groupId       the unique identifier of the group
     * @param memberId      the unique identifier of the member to be removed
     * @param tokenUsername the authentication token or username of the requester
     */
    @Transactional
    public void removeMember(Long groupId, Long memberId, String tokenUsername) {
        User requester = verifyGroupAdmin(groupId, tokenUsername);
        groupService.removeMember(groupId, memberId, requester.getId());
    }

    /**
     * Deletes the entire group and its associated data, provided the requester is a verified group admin.
     *
     * @param groupId       the unique identifier of the group to delete
     * @param tokenUsername the authentication token or username of the requester
     */
    @Transactional
    public void deleteGroup(Long groupId, String tokenUsername) {
        User requester = verifyGroupAdmin(groupId, tokenUsername);
        groupService.deleteGroup(groupId, requester.getId());
    }

    /**
     * Updates the basic details of the group, such as its display name.
     *
     * @param groupId       the unique identifier of the group to update
     * @param newName       the new name to assign to the group
     * @param tokenUsername the authentication token or username of the requester
     */
    @Transactional
    public void updateGroupName(Long groupId, String newName, String tokenUsername) {
        User requester = verifyGroupAdmin(groupId, tokenUsername);
        groupService.updateGroupName(groupId, newName, requester.getId());
    }

    /**
     * Regenerates the invitation code for the group, invalidating any previous codes.
     *
     * @param groupId       the unique identifier of the group
     * @param tokenUsername the authentication token or username of the requester
     * @return the newly generated invitation code
     */
    @Transactional
    public String regenerateInviteCode(Long groupId, String tokenUsername) {
        User requester = verifyGroupAdmin(groupId, tokenUsername);
        return groupService.regenerateInviteCode(groupId, requester.getId());
    }

    /**
     * Transfers administrative ownership of the group to another existing member.
     *
     * @param groupId       the unique identifier of the group
     * @param newAdminId    the unique identifier of the member receiving ownership
     * @param tokenUsername the authentication token or username of the current owner
     */
    @Transactional
    public void transferOwnership(Long groupId, Long newAdminId, String tokenUsername) {
        User requester = verifyGroupAdmin(groupId, tokenUsername);
        groupService.transferOwnership(groupId, requester.getId(), newAdminId);
    }

    /**
     * Allows a group administrator to manually override a selected team for an existing pick entry within their group.
     *
     * @param groupId       the unique identifier of the group where the pick resides
     * @param pickId        the unique identifier of the pick database record
     * @param correctedTeam the new team string to assign to the user's pick
     * @param tokenUsername the authentication token or username of the group administrator
     * @return the updated and saved Pick entity
     */
    @Transactional
    public Pick overridePickSelection(Long groupId, Long pickId, String correctedTeam, String tokenUsername) {
        verifyGroupAdmin(groupId, tokenUsername);
        
        Pick pick = pickRepository.findById(pickId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pick not found"));

        pick.setSelectedTeam(correctedTeam);
        return pickRepository.save(pick);
    }
}