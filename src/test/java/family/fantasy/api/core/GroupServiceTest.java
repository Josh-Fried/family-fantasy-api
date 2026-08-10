package family.fantasy.api.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}