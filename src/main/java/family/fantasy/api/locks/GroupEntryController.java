package family.fantasy.api.locks;

import family.fantasy.api.core.Group;
import family.fantasy.api.core.GroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for managing group entries and leaderboards.
 * Allows users to submit their pick entries into family leagues and 
 * fetches the sorted leaderboard data for the frontend.
 */
@RestController
@RequestMapping("/api/v1/group-entries")
public class GroupEntryController {

    private final GroupEntryRepository groupEntryRepository;
    private final GroupRepository groupRepository;
    private final PickEntryRepository pickEntryRepository;

    public GroupEntryController(GroupEntryRepository groupEntryRepository, GroupRepository groupRepository, PickEntryRepository pickEntryRepository) {
        this.groupEntryRepository = groupEntryRepository;
        this.groupRepository = groupRepository;
        this.pickEntryRepository = pickEntryRepository;
    }

    @PostMapping("/group/{groupId}/entry/{entryId}")
    public ResponseEntity<GroupEntry> linkEntryToGroup(
            @PathVariable Long groupId,
            @PathVariable Long entryId) {
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        PickEntry pickEntry = pickEntryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Pick Entry not found"));
                
        GroupEntry groupEntry = new GroupEntry(group, pickEntry);
        GroupEntry savedEntry = groupEntryRepository.save(groupEntry);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntry);
    }

    @GetMapping("/group/{groupId}/leaderboard")
    public ResponseEntity<List<GroupEntry>> getGroupLeaderboard(@PathVariable Long groupId) {
        List<GroupEntry> leaderboard = groupEntryRepository.findByGroupIdOrderByPickEntryTotalScoreDesc(groupId);
        return ResponseEntity.ok(leaderboard);
    }
}