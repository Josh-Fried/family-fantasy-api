package family.fantasy.api.locks;

import family.fantasy.api.core.Group;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GroupEntryService {

    private final GroupEntryRepository groupEntryRepository;

    public GroupEntryService(GroupEntryRepository groupEntryRepository) {
        this.groupEntryRepository = groupEntryRepository;
    }

    public GroupEntry enterGroup(Group group, PickEntry pickEntry) {
        GroupEntry entry = new GroupEntry(group, pickEntry);
        return groupEntryRepository.save(entry);
    }

    public List<GroupEntry> getGroupLeaderboard(Long groupId) {
        return groupEntryRepository.findByGroupIdOrderByPickEntryTotalScoreDesc(groupId);
    }
}