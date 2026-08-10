package family.fantasy.api.locks;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public AdminController(PickEntryRepository pickEntryRepository, PickRepository pickRepository) {
        this.pickEntryRepository = pickEntryRepository;
        this.pickRepository = pickRepository;
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
}