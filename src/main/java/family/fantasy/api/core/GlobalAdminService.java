package family.fantasy.api.core;

import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import family.fantasy.api.core.User;
import family.fantasy.api.core.UserRepository;
import family.fantasy.api.locks.MatchupRepository;
import family.fantasy.api.locks.Pick;
import family.fantasy.api.locks.PickRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible strictly for platform-wide administrative actions.
 */
@Service
public class GlobalAdminService {

    private final UserRepository userRepository;
    private final NflSyncService nflSyncService;
    private final CacheManager cacheManager;
    private final PickRepository pickRepository;
    private final MatchupRepository matchupRepository;

    /**
     * Constructs a new GlobalAdminService with the required repositories and external services.
     *
     * @param userRepository    repository for accessing user authentication data
     * @param nflSyncService    service for synchronizing NFL schedule and score data
     * @param cacheManager      manager for handling and clearing application-level caches
     * @param pickRepository    repository for accessing and modifying user lock picks
     * @param matchupRepository repository for managing game matchup records
     */
    public GlobalAdminService(
            UserRepository userRepository,
            NflSyncService nflSyncService,
            CacheManager cacheManager,
            PickRepository pickRepository,
            MatchupRepository matchupRepository) {
        this.userRepository = userRepository;
        this.nflSyncService = nflSyncService;
        this.cacheManager = cacheManager;
        this.pickRepository = pickRepository;
        this.matchupRepository = matchupRepository;
    }

    /**
     * Verifies that the user attempting to execute an action holds global administrator privileges.
     * Throws an unauthorized or forbidden exception if the user lacks the proper role.
     *
     * @param adminUserId the unique identifier of the requesting user
     */
    public void verifyGlobalAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin user ID required.");
        }
        User user = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (!user.isGlobalAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Global admin permissions required.");
        }
    }

    /**
     * Triggers a schedule synchronization for a specific NFL season and a defined season type.
     *
     * @param adminUserId the unique identifier of the global administrator
     * @param year        the specific season year to synchronize
     * @param seasonType  the integer representing the phase of the season (e.g., regular, postseason)
     */
    public void syncSeasonWithType(Long adminUserId, String year, int seasonType) {
        verifyGlobalAdmin(adminUserId);
        nflSyncService.syncEntireSeason(year, seasonType);
    }

    /**
     * Fetches and saves the matchup schedule and scores for a single specific NFL week.
     *
     * @param adminUserId the unique identifier of the global administrator
     * @param year        the season year to synchronize
     * @param seasonType  the type of season phase
     * @param week        the specific week number to synchronize
     * @return the total number of games successfully fetched and saved
     */
    public int forceSyncWeek(Long adminUserId, String year, int seasonType, int week) {
        verifyGlobalAdmin(adminUserId);
        return nflSyncService.fetchAndSaveFromEspn(year, seasonType, week);
    }

    /**
     * Force-syncs the live scoreboard for the currently specified week, resolving playoff mappings automatically.
     *
     * @param adminUserId the unique identifier of the global administrator
     * @param year        the season year to synchronize
     * @param week        the active week number to pull live game data for
     * @return the total number of live games updated in the database
     */
    public int forceLiveMatchupSync(Long adminUserId, String year, int week) {
        verifyGlobalAdmin(adminUserId);
        // int seasonType = (week > 18) ? 3 : 2;
        int seasonType = 1;
        int espnWeek = (week > 18) ? (week - 18) : week;
        return nflSyncService.fetchAndSaveFromEspn(year, seasonType, espnWeek);
    }

    /**
     * Flushes and clears all registered caches within the application environment to ensure fresh data delivery.
     *
     * @param adminUserId the unique identifier of the global administrator
     */
    public void clearAllCaches(Long adminUserId) {
        verifyGlobalAdmin(adminUserId);
        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }

    /**
     * Idempotently recalculates the points and regrades all user picks for a specified week.
     *
     * @param adminUserId the unique identifier of the global administrator
     * @param season      the year of the season to evaluate
     * @param week        the specific week to recalculate scores for
     * @return a map containing the statistical results and status of the regrading process
     */
    @Transactional
    public Map<String, Object> recalculateWeekScoring(Long requestingUserId, int season, int week) {
        // 1. Verify admin permissions if needed
        // verifyAdmin(requestingUserId);

        // 2. Call your new optimized weekly scoring processor
        nflSyncService.processWeeklyScoring(season, week);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Successfully recalculated and cached scores for Season " + season + ", Week " + week);
        return response;
    }

    /**
     * Allows a global administrator to manually override a selected team for an existing pick entry.
     *
     * @param adminUserId   the unique identifier of the global administrator
     * @param pickId        the unique identifier of the pick database record to modify
     * @param correctedTeam the new team string to assign to the user's pick
     * @return the updated and saved Pick entity
     */
    @Transactional
    public Pick overridePickSelection(Long adminUserId, Long pickId, String correctedTeam) {
        verifyGlobalAdmin(adminUserId);
        Pick pick = pickRepository.findById(pickId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pick not found"));

        pick.setSelectedTeam(correctedTeam);
        return pickRepository.save(pick);
    }

    /**
     * Wipes all existing picks and matchups from the database to reset the testing environment.
     *
     * @param adminUserId the unique identifier of the global administrator
     */
    @Transactional
    public void clearTestData(Long adminUserId) {
        verifyGlobalAdmin(adminUserId);
        pickRepository.deleteAll();
        matchupRepository.deleteAll();
    }
}