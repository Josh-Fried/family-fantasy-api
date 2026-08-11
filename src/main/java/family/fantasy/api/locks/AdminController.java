package family.fantasy.api.locks;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import family.fantasy.api.core.NflSyncService;

/**
 * REST controller for administrative commissioner actions.
 * Allows for manual score overrides and correcting team picks 
 * if a league member makes an error or a rule dispute occurs.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final PickEntryRepository pickEntryRepository;
    private final PickRepository pickRepository;
    private final NflSyncService nflSyncService;

    public AdminController(PickEntryRepository pickEntryRepository, PickRepository pickRepository, NflSyncService nflSyncService) {
        this.pickEntryRepository = pickEntryRepository;
        this.pickRepository = pickRepository;
        this.nflSyncService = nflSyncService;
    }

    @PutMapping("/entries/{entryId}/override-score")
    public ResponseEntity<PickEntry> overrideTotalScore(
            @PathVariable Long entryId,
            @RequestParam Integer newScore) {
        
        PickEntry entry = pickEntryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Pick Entry not found"));
                
        entry.setTotalScore(newScore);
        PickEntry updatedEntry = pickEntryRepository.save(entry);
        
        return ResponseEntity.ok(updatedEntry);
    }

    @PutMapping("/picks/{pickId}/override-team")
    public ResponseEntity<Pick> overridePickSelection(
            @PathVariable Long pickId,
            @RequestParam String correctedTeam) {
        
        Pick pick = pickRepository.findById(pickId)
                .orElseThrow(() -> new RuntimeException("Pick not found"));
                
        pick.setSelectedTeam(correctedTeam);
        Pick updatedPick = pickRepository.save(pick);
        
        return ResponseEntity.ok(updatedPick);
    }

    /**
     * Download a week of Matchups.
     * 
     * To run this, just open your web browser and go to:
     * http://localhost:8080/api/v1/admin/sync/week/1
     */
    @GetMapping("/sync/week/{week}")
    public ResponseEntity<String> forceSyncWeek(@PathVariable int week) {
        
        // Calls our fetch method directly for the specific week requested
        nflSyncService.fetchAndSaveFromEspn("2026", 2, week);
        
        return ResponseEntity.ok("Successfully triggered sync for Week " + week + ". Check your Java console!");
    }
    /**
     * Download the entire season schedule at once!
     * To run this, open your web browser and go to:
     * http://localhost:8080/api/v1/admin/sync/season/2026
     */
    @GetMapping("/sync/season/{year}")
    public ResponseEntity<String> forceSyncSeason(@PathVariable String year) {
        
        nflSyncService.syncEntireSeason(year);
        
        return ResponseEntity.ok("Successfully downloaded all 18 weeks of the " + year + " schedule! Check console.");
    }
}

