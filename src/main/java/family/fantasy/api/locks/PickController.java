package family.fantasy.api.locks;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for the Locks game domain.
 * This handles fetching the weekly football matchups and allows users to submit 
 * their picks for specific entries while enforcing kickoff time deadlines.
 */
@RestController
@RequestMapping("/api/v1/locks")
public class PickController {

    private final MatchupService matchupService;
    private final PickService pickService;
    private final PickEntryRepository pickEntryRepository;

    public PickController(MatchupService matchupService, PickService pickService, PickEntryRepository pickEntryRepository) {
        this.matchupService = matchupService;
        this.pickService = pickService;
        this.pickEntryRepository = pickEntryRepository;
    }

    @GetMapping("/matchups/season/{season}/week/{week}")
    public ResponseEntity<List<Matchup>> getWeeklyMatchups(
            @PathVariable Integer season, 
            @PathVariable Integer week) {
        
        List<Matchup> matchups = matchupService.getWeeklyMatchups(season, week);
        return ResponseEntity.ok(matchups);
    }

    @PostMapping("/entry/{entryId}/matchup/{matchupId}")
    public ResponseEntity<Pick> submitPick(
            @PathVariable Long entryId,
            @PathVariable Long matchupId,
            @RequestParam String selectedTeam) {
        
        PickEntry entry = pickEntryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Entry not found"));
        Pick savedPick = pickService.submitPick(entry, matchupId, selectedTeam);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPick);
    }
}