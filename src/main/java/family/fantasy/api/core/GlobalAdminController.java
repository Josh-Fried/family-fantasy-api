package family.fantasy.api.core;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import family.fantasy.api.locks.Pick;

import java.util.Map;

/**
 * Centralized Administrative Controller for Platform-Wide System Management.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class GlobalAdminController {

    private final GlobalAdminService globalAdminService;

    /**
     * Constructs a new GlobalAdminController with the underlying global admin service layer.
     *
     * @param globalAdminService the service encapsulating all platform-wide administrative logic
     */
    public GlobalAdminController(GlobalAdminService globalAdminService) {
        this.globalAdminService = globalAdminService;
    }

    /**
     * Fetches and saves the matchup schedule and scores for a specific NFL week.
     *
     * @param currentUser the verified and authenticated user from the security context
     * @param week        the specific week number to synchronize
     * @param seasonType  the type of season, defaulting to the regular season
     * @return a response entity outlining the number of games successfully saved
     */
    @PostMapping("/locks/sync/week/{week}")
    public ResponseEntity<?> forceSyncWeek(
            @AuthenticationPrincipal User currentUser,
            @PathVariable int week,
            @RequestParam(defaultValue = "2") int seasonType) {
        
        int gamesSaved = globalAdminService.forceSyncWeek(currentUser.getId(), "2026", seasonType, week);
        
        return ResponseEntity.ok(Map.of(
                "message", "Successfully synced Week " + week,
                "gamesSaved", gamesSaved
        ));
    }

    /**
     * Triggers a schedule sync for a specific NFL season and season type.
     *
     * @param currentUser the verified and authenticated user from the security context
     * @param year        the specific season year to synchronize
     * @param seasonType  the integer representing the phase of the season
     * @return a response entity containing a success message
     */
    @PostMapping("/sync/season/{year}/{seasonType}")
    public ResponseEntity<?> forceSyncSeasonWithType(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String year,
            @PathVariable int seasonType) {
        globalAdminService.syncSeasonWithType(currentUser.getId(), year, seasonType);
        return ResponseEntity.ok(Map.of(
                "message", "Successfully downloaded season " + year + " for type " + seasonType
        ));
    }

    /**
     * Flushes and clears all registered caches within the application environment.
     *
     * @param currentUser the verified and authenticated user from the security context
     * @return a response entity confirming the caches have been cleared
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearAllCaches(@AuthenticationPrincipal User currentUser) {
        globalAdminService.clearAllCaches(currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "All application caches successfully cleared."));
    }

    /**
     * Force-syncs the live scoreboard for the currently specified week.
     *
     * @param currentUser the verified and authenticated user from the security context
     * @param year        the season year to synchronize
     * @param week        the active week number to pull live game data for
     * @return a response entity displaying the total number of games updated
     */
    @PostMapping("/locks/matchups/force-live-sync")
    public ResponseEntity<?> forceLiveMatchupSync(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String year,
            @RequestParam int week) {
            
        int gamesUpdated = globalAdminService.forceLiveMatchupSync(currentUser.getId(), year, week);
        return ResponseEntity.ok(Map.of(
                "message", "Live scoreboard force-synced successfully.",
                "gamesUpdated", gamesUpdated
        ));
    }

    /**
     * Idempotently recalculates the points and regrades all user picks for a specified week.
     *
     * @param currentUser the verified and authenticated user from the security context
     * @param season      the year of the season to evaluate
     * @param week        the specific week to recalculate scores for
     * @return a response entity featuring the statistical results of the regrading process
     */
    @PostMapping("/locks/scoring/recalculate")
    public ResponseEntity<?> rerunWeeklyScoring(
            @AuthenticationPrincipal User currentUser,
            @RequestParam int season,
            @RequestParam int week) {
            
        Map<String, Object> result = globalAdminService.recalculateWeekScoring(currentUser.getId(), season, week);
        return ResponseEntity.ok(result);
    }

    /**
     * Allows a global administrator to manually override a selected team for an existing pick entry.
     *
     * @param currentUser   the verified and authenticated user from the security context
     * @param pickId        the unique identifier of the pick database record
     * @param correctedTeam the new team string to assign to the user's pick
     * @return a response entity delivering the updated pick record
     */
    @PutMapping("/locks/picks/{pickId}/override-team")
    public ResponseEntity<Pick> overridePickSelection(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long pickId,
            @RequestParam String correctedTeam) {
            
        Pick updatedPick = globalAdminService.overridePickSelection(currentUser.getId(), pickId, correctedTeam);
        return ResponseEntity.ok(updatedPick);
    }

    /**
     * Wipes all existing picks and matchups from the database to reset the testing environment.
     *
     * @param currentUser the verified and authenticated user from the security context
     * @return a response entity confirming the test data removal
     */
    @PostMapping("/locks/clear-test-data")
    public ResponseEntity<?> clearTestData(@AuthenticationPrincipal User currentUser) {
        globalAdminService.clearTestData(currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "All picks and matchups wiped for a clean test run."));
    }
}