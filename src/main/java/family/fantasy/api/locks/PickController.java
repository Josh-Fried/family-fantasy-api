package family.fantasy.api.locks;

import family.fantasy.api.core.User;
import family.fantasy.api.core.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/picks")
public class PickController {

    private final PickService pickService;
    private final PickRepository pickRepository;
    private final UserRepository userRepository;

    public PickController(PickService pickService, PickRepository pickRepository, UserRepository userRepository) {
        this.pickService = pickService;
        this.pickRepository = pickRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> submitPick(
            @PathVariable Long userId,
            @RequestBody PickRequest request) {
        
        try {
            // Find the user making the pick
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            // Pass it to the service logic to validate the kickoff time
            Pick savedPick = pickService.submitPick(user, request.matchupId(), request.selectedTeam());
            return ResponseEntity.ok(savedPick);
            
        } catch (RuntimeException e) {
            // If the kickoff time passed, return a 400 Bad Request with the error message
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // @GetMapping("/user/{userId}/season/{season}/week/{week}")
    // public ResponseEntity<List<Pick>> getUserPicksForWeek(
    //         @PathVariable Long userId,
    //         @PathVariable Integer season,
    //         @PathVariable Integer week) {
        
    //     // Grabs only the picks for this specific week so React can paint them green/red on the screen
    //     List<Pick> picks = pickRepository.findByUserIdAndMatchupSeasonAndMatchupWeekNumber(userId, season, week);
    //     return ResponseEntity.ok(picks);
    // }

    // Triggers when the user clicks a team they already selected
    @DeleteMapping("/user/{userId}/matchup/{matchupId}")
    public ResponseEntity<?> removePick(
            @PathVariable Long userId,
            @PathVariable Long matchupId) {
        try {
            pickService.removePick(userId, matchupId);
            return ResponseEntity.ok("Pick successfully removed.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // A lightweight Record to capture the JSON coming from the React frontend
    public record PickRequest(Long matchupId, String selectedTeam) {}

    @GetMapping("/user/{targetUserId}/season/{season}/week/{week}")
    public ResponseEntity<List<Pick>> getUserPicks(
            @PathVariable Long targetUserId,
            @PathVariable int season,
            @PathVariable int week,
            @RequestParam(required = false) Long requestingUserId) { // <--- Added requesting user

        // If requestingUserId is null, we assume they are unauthenticated and block them
        if (requestingUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Pick> picks = pickService.getPicksSecurely(targetUserId, requestingUserId, season, week);
        return ResponseEntity.ok(picks);
    }
}