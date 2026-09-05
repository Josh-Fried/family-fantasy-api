package family.fantasy.api.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import family.fantasy.api.core.NflStateService.NflState;
import family.fantasy.api.locks.Matchup;
import family.fantasy.api.locks.MatchupRepository;
import family.fantasy.api.locks.Pick;
import family.fantasy.api.locks.PickRepository;
import family.fantasy.api.locks.UserWeeklyScore;
import family.fantasy.api.locks.UserWeeklyScoreRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service responsible for orchestrating NFL schedule synchronization, 
 * fetching live game outcomes from ESPN, updating matchup statuses, 
 * and evaluating user picks to compute aggregated weekly scores.
 */
@Service
public class NflSyncService {

    private final RestTemplate restTemplate;
    private final MatchupRepository matchupRepository;
    private final PickRepository pickRepository;
    private final NflStateService nflStateService;
    private final UserWeeklyScoreRepository userWeeklyScoreRepository;
    private final UserRepository userRepository; 

    public NflSyncService(RestTemplate restTemplate, 
                          MatchupRepository matchupRepository, 
                          PickRepository pickRepository, 
                          NflStateService nflStateService, 
                          UserWeeklyScoreRepository userWeeklyScoreRepository,
                          UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.matchupRepository = matchupRepository;
        this.pickRepository = pickRepository;
        this.nflStateService = nflStateService;
        this.userWeeklyScoreRepository = userWeeklyScoreRepository;
        this.userRepository = userRepository;
    }

    /**
     * Scheduled job that frequently triggers live scoring updates from ESPN on game days.
     * It runs every 15 minutes during standard NFL windows on Sunday, Monday, and Thursday.
     */
    @Scheduled(cron = "0 0/15 13-23 * * SUN")
    @Scheduled(cron = "0 0/15 17-23 * * MON,THU")
    public void liveScoreUpdates() {
        System.out.println("🏈 [LIVE SYNC] Waking up to update live game scores and check winners...");
        syncCurrentWeek();
    }

    /**
     * Scheduled job running early Tuesday morning to perform a final sweep of the week's games.
     * It clears all relevant leaderboards and state caches to ensure fresh rendering 
     * once stat corrections and finalized outcomes are logged.
     */
    @Scheduled(cron = "0 0 6 * * TUE")
    @Caching(evict = {
        @CacheEvict(value = "nflState", allEntries = true),
        @CacheEvict(value = "groupLeaderboards", allEntries = true),
        @CacheEvict(value = "groupDetails", allEntries = true),
        @CacheEvict(value = "userTotalScores", allEntries = true)
    })
    public void tuesdayMorningWrapUp() {
        System.out.println("🧹 [TUESDAY WRAP-UP] Doing final check on week scores...");
        syncCurrentWeek();
    }

    /**
     * Fetches the active NFL state to determine the current week and season context,
     * translates the season configuration to match ESPN's API parameters, and 
     * delegates the network call to retrieve the latest live data.
     */
    private void syncCurrentWeek() {
        try {
            NflState state = nflStateService.getNflState();
            
            int espnSeasonType = 2; // Default to regular season
            if ("pre".equalsIgnoreCase(state.seasonType())) {
                espnSeasonType = 1;
            } else if ("post".equalsIgnoreCase(state.seasonType())) {
                espnSeasonType = 3;
            }
            
            fetchAndSaveFromEspn(String.valueOf(state.season()), espnSeasonType, state.week());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to sync current NFL week: " + e.getMessage());
        }
    }

    /**
     * Batch utility primarily used by administrators to populate or repair an entire season 
     * of matchups by iterating through all possible weeks sequentially and fetching ESPN records.
     */
    public void syncEntireSeason(String year, Integer seasonType) {
        int type = (seasonType != null) ? seasonType : 2;
        String seasonTypeString;
        int maxWeeks;

        if (type == 1) {
            seasonTypeString = "preseason";
            maxWeeks = 4;
        } else if (type == 3) {
            seasonTypeString = "postseason";
            maxWeeks = 5;
        } else {
            seasonTypeString = "regular season";
            maxWeeks = 18;
        }
        
        System.out.println("🚀 Commencing full " + seasonTypeString + " download for " + year + "...");
        int totalGamesSaved = 0;
        
        for (int week = 1; week <= maxWeeks; week++) {
            totalGamesSaved += fetchAndSaveFromEspn(year, type, week); 
            try { 
                Thread.sleep(1000); 
            } catch (InterruptedException e) { 
                Thread.currentThread().interrupt(); 
            }
        }
        
        System.out.println("✅ Finished! Total " + year + " " + seasonTypeString + " games saved/updated in database: " + totalGamesSaved);
    }

    /**
     * Reaches out to the external ESPN scoreboard API to capture schedules, scores, 
     * team designations, and final game states. Translates incoming JSON into local Matchup 
     * entities, persisting new games and updating ongoing or finalized matches.
     */
    public int fetchAndSaveFromEspn(String year, int seasonType, int week) {
        int gamesSavedThisWeek = 0;
        int actualDatabaseWeek = week;
        if (seasonType == 3) { 
            actualDatabaseWeek = week + 18;
        }
        
        try {
            String url = "https://site.web.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard?year=" + year + "&seasontype=" + seasonType + "&week=" + week;
            String jsonResponse = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            if (root != null && root.has("events")) {
                JsonNode events = root.path("events");
                System.out.println("📡 Week " + week + " - Found " + events.size() + " games from ESPN.");
                
                for (JsonNode event : events) {
                    String gameId = event.path("id").asText("UNKNOWN_ID");
                    String status = event.path("status").path("type").path("name").asText("STATUS_UNKNOWN");
                    
                    String dateString = event.path("date").asText(null);
                    OffsetDateTime kickoffTime = dateString != null ? OffsetDateTime.parse(dateString) : null;

                    JsonNode competitors = event.path("competitions").path(0).path("competitors");
                    String homeTeam = "Unknown Home";
                    String awayTeam = "Unknown Away";
                    int homeScore = 0;
                    int awayScore = 0;

                    if (competitors.isArray()) {
                        for (JsonNode teamNode : competitors) {
                            String teamName = teamNode.path("team").path("displayName").asText("Unknown Team");
                            String homeAway = teamNode.path("homeAway").asText("");
                            int score = teamNode.path("score").asInt(0);

                            if (homeAway.equals("home")) {
                                homeTeam = teamName;
                                homeScore = score;
                            } else if (homeAway.equals("away")) {
                                awayTeam = teamName;
                                awayScore = score;
                            }
                        }
                    }

                    String winningTeam = null;
                    if ("STATUS_FINAL".equals(status)) {
                        if (homeScore > awayScore) {
                            winningTeam = homeTeam;
                        } else if (awayScore > homeScore) {
                            winningTeam = awayTeam;
                        } else {
                            winningTeam = "TIE";
                        }
                    }

                    Matchup existingMatchup = matchupRepository.findByExternalId(gameId).orElse(null);
                    
                    if (existingMatchup == null) {
                        Matchup newMatchup = new Matchup();
                        newMatchup.setExternalId(gameId);
                        newMatchup.setSeason(Integer.parseInt(year)); 
                        newMatchup.setHomeTeam(homeTeam);
                        newMatchup.setAwayTeam(awayTeam);
                        newMatchup.setWeekNumber(actualDatabaseWeek);
                        newMatchup.setStatus(status);
                        newMatchup.setHomeScore(homeScore);
                        newMatchup.setAwayScore(awayScore);
                        newMatchup.setWinningTeam(winningTeam);
                        newMatchup.setKickoffTime(kickoffTime); 
                        matchupRepository.save(newMatchup);
                        gamesSavedThisWeek++;
                    } else {
                        boolean wasAlreadyFinal = "STATUS_FINAL".equals(existingMatchup.getStatus());
                        
                        existingMatchup.setStatus(status);
                        existingMatchup.setHomeScore(homeScore);
                        existingMatchup.setAwayScore(awayScore);
                        existingMatchup.setWinningTeam(winningTeam);
                        existingMatchup.setKickoffTime(kickoffTime);
                        existingMatchup.setSeason(Integer.parseInt(year));
                        matchupRepository.save(existingMatchup);
                        gamesSavedThisWeek++;

                        if (!wasAlreadyFinal && "STATUS_FINAL".equals(status)) {
                            System.out.println("🏁 Game went final: " + awayTeam + " (" + awayScore + ") @ " + homeTeam + " (" + homeScore + ")");
                            gradeMatchup(existingMatchup);
                        }
                    }
                }
            } else {
                 System.out.println("⚠️ Week " + week + " - No events array found in ESPN response! (Is the year correct?)");
            }
        } catch (Exception e) {
            System.err.println("❌ FAILED to sync ESPN schedule for Week " + week + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return gamesSavedThisWeek;
    }

    /**
     * Iterates over all database records tied to a finalized matchup and flags individual user
     * picks as true or false based strictly on whether their selected team exactly matches the outcome.
     */
    private void gradeMatchup(Matchup matchup) {
        List<Pick> picks = pickRepository.findByMatchupId(matchup.getId());

        for (Pick pick : picks) {
            if (pick.getSelectedTeam().equalsIgnoreCase(matchup.getWinningTeam())) {
                pick.setIsCorrect(true);
            } else {
                pick.setIsCorrect(false);
            }
            pickRepository.save(pick);
        }
        
        System.out.println("✅ Graded " + picks.size() + " user picks for game: " + matchup.getAwayTeam() + " @ " + matchup.getHomeTeam());
    }

    /**
     * Executes the strict all-or-nothing scoring rule across the user base for a particular week.
     * Evaluates if a user picked every single completed game correctly, computes their point 
     * accumulation, stores the summarized total in the database, and evicts stale leaderboard caches.
     */
    @Transactional
    @CacheEvict(value = {"userTotalScores", "groupLeaderboards"}, allEntries = true)
    public void processWeeklyScoring(int season, int week) {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            List<Pick> weekPicks = pickRepository.findByUserIdAndMatchupSeasonAndMatchupWeekNumber(user.getId(), season, week);

            if (weekPicks.isEmpty()) {
                continue;
            }

            int correctCount = 0;
            boolean hasIncorrectPick = false;

            for (Pick pick : weekPicks) {
                Matchup matchup = pick.getMatchup();
                if (matchup == null || !"STATUS_FINAL".equals(matchup.getStatus())) {
                    continue;
                }

                String selected = pick.getSelectedTeam();
                String winner = matchup.getWinningTeam();

                if (selected != null && winner != null && selected.equalsIgnoreCase(winner)) {
                    correctCount++;
                } else {
                    hasIncorrectPick = true;
                }
            }

            int pointsEarned = hasIncorrectPick ? 0 : correctCount;

            UserWeeklyScore weeklyScore = userWeeklyScoreRepository
                .findByUserIdAndSeasonAndWeekNumber(user.getId(), season, week)
                .orElseGet(() -> {
                    UserWeeklyScore newScore = new UserWeeklyScore();
                    newScore.setUser(user);
                    newScore.setSeason(season);
                    newScore.setWeekNumber(week);
                    return newScore;
                });

            weeklyScore.setPointsEarned(pointsEarned);
            weeklyScore.setPerfectWeek(!hasIncorrectPick && correctCount > 0);
            userWeeklyScoreRepository.save(weeklyScore);
        }
    }

    /**
     * Retrieves the fully aggregated points total for a distinct user utilizing a highly efficient 
     * sum query against weekly summaries, serving subsequent reads directly from memory via caching.
     */
    @Cacheable(value = "userTotalScores", key = "#userId + '-' + #season")
    public int getUserTotalScore(Long userId, int season) {
        return userWeeklyScoreRepository.findTotalScoreByUserIdAndSeason(userId, season);
    }
}