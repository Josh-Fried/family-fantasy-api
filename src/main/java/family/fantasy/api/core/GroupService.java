package family.fantasy.api.core;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    public GroupService(GroupRepository groupRepository, UserGroupRepository userGroupRepository) {
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
    }

    public Group createGroup(User user, String groupName, String gameTypeString) {
        // System.out.println("Service: " + gameTypeString);
        Group newGroup = new Group(groupName);
        newGroup.setGameType(Group.GameType.fromString(gameTypeString));
        Group savedGroup = groupRepository.save(newGroup);
        UserGroup userGroup = new UserGroup(user, savedGroup, true);
        userGroupRepository.save(userGroup);
        return savedGroup;
    }

    public Group joinGroup(User user, String inviteCode) {
        Group group = groupRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new RuntimeException("Invalid invite code"));
        UserGroup userGroup = new UserGroup(user, group, false);
        userGroupRepository.save(userGroup);
        return group;
    }

    public List<UserGroup> getUserGroups(Long userId) {
        return userGroupRepository.findByUserId(userId);
    }
}