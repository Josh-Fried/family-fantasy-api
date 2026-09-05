package family.fantasy.api.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @InjectMocks
    private GroupService groupService;

    /**
     * Tests that creating a group saves the new group to the database and 
     * immediately assigns the creating user as an admin in the cross-reference table.
     */
    @Test
    void createGroup_ValidInputs_SavesGroupAndUserGroup() {
        User user = new User("test@email.com", "User");
        Group mockGroup = new Group("Family League");
        when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);

        Group result = groupService.createGroup(user, "Family League", "LOCKS");

        assertNotNull(result);
        verify(groupRepository, times(1)).save(any(Group.class));
        verify(userGroupRepository, times(1)).save(any(UserGroup.class));
    }

    /**
     * Tests that a user is successfully added to an existing group when 
     * providing a valid invite code.
     */
    @Test
    void joinGroup_ValidInviteCode_SavesUserGroup() {
        User user = new User("test@email.com", "User");
        Group mockGroup = new Group("Family League");
        when(groupRepository.findByInviteCode("VALIDCODE")).thenReturn(Optional.of(mockGroup));

        Group result = groupService.joinGroup(user, "VALIDCODE");

        assertEquals(mockGroup, result);
        verify(userGroupRepository, times(1)).save(any(UserGroup.class));
    }

    /**
     * Tests that isAdmin returns true when the user group record has administrator privileges set to true.
     */
    @Test
    void isAdmin_UserIsAdmin_ReturnsTrue() {
        Long groupId = 1L;
        Long userId = 10L;
        User user = new User("test@email.com", "User");
        Group group = new Group("Family League");
        UserGroup userGroup = new UserGroup(user, group, true);

        when(userGroupRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(userGroup));

        boolean result = groupService.isAdmin(groupId, userId);

        assertTrue(result);
    }

    /**
     * Tests that isAdmin returns false when the user group record has administrator privileges set to false.
     */
    @Test
    void isAdmin_UserIsNotAdmin_ReturnsFalse() {
        Long groupId = 1L;
        Long userId = 10L;
        User user = new User("test@email.com", "User");
        Group group = new Group("Family League");
        UserGroup userGroup = new UserGroup(user, group, false);

        when(userGroupRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(userGroup));

        boolean result = groupService.isAdmin(groupId, userId);

        assertFalse(result);
    }

    /**
     * Tests that updating the group name successfully updates and saves the group entity.
     */
    @Test
    void updateGroupName_ValidGroup_UpdatesName() {
        Long groupId = 1L;
        Long requesterId = 10L;
        Group group = new Group("Old Name");
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        groupService.updateGroupName(groupId, "New Name", requesterId);

        assertEquals("New Name", group.getName());
        verify(groupRepository, times(1)).save(group);
    }

    /**
     * Tests that regenerating the invite code updates the invite code string and saves the group.
     */
    @Test
    void regenerateInviteCode_ValidGroup_UpdatesInviteCode() {
        Long groupId = 1L;
        Long requesterId = 10L;
        Group group = new Group("Family League");
        String oldCode = group.getInviteCode();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        String newCode = groupService.regenerateInviteCode(groupId, requesterId);

        assertNotNull(newCode);
        assertNotEquals(oldCode, newCode);
        assertEquals(newCode, group.getInviteCode());
        verify(groupRepository, times(1)).save(group);
    }

    /**
     * Tests that transferring ownership successfully revokes admin status from the current owner 
     * and grants it to the new administrator.
     */
    @Test
    void transferOwnership_ValidMembers_TransfersAdminStatus() {
        Long groupId = 1L;
        Long currentOwnerId = 10L;
        Long newAdminId = 20L;
        
        User ownerUser = new User("owner@email.com", "Owner");
        User newAdminUser = new User("newadmin@email.com", "NewAdmin");
        Group group = new Group("Family League");

        UserGroup ownerGroup = new UserGroup(ownerUser, group, true);
        UserGroup newAdminGroup = new UserGroup(newAdminUser, group, false);

        when(userGroupRepository.findByGroupIdAndUserId(groupId, currentOwnerId)).thenReturn(Optional.of(ownerGroup));
        when(userGroupRepository.findByGroupIdAndUserId(groupId, newAdminId)).thenReturn(Optional.of(newAdminGroup));

        groupService.transferOwnership(groupId, currentOwnerId, newAdminId);

        assertFalse(ownerGroup.getIsAdmin());
        assertTrue(newAdminGroup.getIsAdmin());
        verify(userGroupRepository, times(1)).save(ownerGroup);
        verify(userGroupRepository, times(1)).save(newAdminGroup);
    }

    /**
     * Tests that transferring ownership throws a bad request exception if the target new admin is not a group member.
     */
    @Test
    void transferOwnership_NewAdminNotMember_ThrowsException() {
        Long groupId = 1L;
        Long currentOwnerId = 10L;
        Long newAdminId = 99L;
        
        User ownerUser = new User("owner@email.com", "Owner");
        Group group = new Group("Family League");
        UserGroup ownerGroup = new UserGroup(ownerUser, group, true);

        when(userGroupRepository.findByGroupIdAndUserId(groupId, currentOwnerId)).thenReturn(Optional.of(ownerGroup));
        when(userGroupRepository.findByGroupIdAndUserId(groupId, newAdminId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            groupService.transferOwnership(groupId, currentOwnerId, newAdminId);
        });
        
        verify(userGroupRepository, never()).save(any(UserGroup.class));
    }
}