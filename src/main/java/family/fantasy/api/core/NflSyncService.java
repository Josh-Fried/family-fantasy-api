package family.fantasy.api.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import family.fantasy.api.core.NflStateService.NflState;
import family.fantasy.api.locks.Matchup;
import family.fantasy.api.locks.MatchupRepository;
import family.fantasy.api.locks.Pick;
import family.fantasy.api.locks.PickRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NflSyncService {

    private final RestTemplate restTemplate;
    private final MatchupRepository matchupRepository;
    private final PickRepository pickRepository;
    private final NflStateService nflStateService;

    public NflSyncService(RestTemplate restTemplate, MatchupRepository matchupRepository, PickRepository pickRepository, NflStateService nflStateService) {
        this.restTemplate = restTemplate;
        this.matchupRepository = matchupRepository;
        this.pickRepository = pickRepository;
        this.nflStateService = nflStateService;
    }

    @Scheduled(cron = "0 0/15 13-23 * * SUN")
    @Scheduled(cron = "0 0/15 17-23 * * MON,THU")
    public void liveScoreUpdates() {
        System.out.println("🏈 [LIVE SYNC] Waking up to update live game scores and check winners...");
        syncCurrentWeek();
    }

    @Scheduled(cron = "0 0 6 * * TUE")
    @Caching(evict = {
        @CacheEvict(value = "nflState", allEntries = true),
        @CacheEvict(value = "groupLeaderboards", allEntries = true),
        @CacheEvict(value = "groupDetails", allEntries = true),
    })
    public void tuesdayMorningWrapUp() {
        System.out.println("🧹 [TUESDAY WRAP-UP] Doing final check on week scores...");
        syncCurrentWeek();
    }

    private void syncCurrentWeek() {
        try {
            NflState state = nflStateService.getNflState();
            
            // Translate Sleeper's seasonType string into ESPN's integer (1 = pre, 2 = regular, 3 = post)
            int espnSeasonType = 2; // Default to regular season
            if ("pre".equalsIgnoreCase(state.seasonType())) {
                espnSeasonType = 1;
            } else if ("post".equalsIgnoreCase(state.seasonType())) {
                espnSeasonType = 3;
            }
            
            // Pass the cached data into your existing ESPN fetcher
            fetchAndSaveFromEspn(String.valueOf(state.season()), espnSeasonType, state.week());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to sync current NFL week: " + e.getMessage());
        }
    }

    public void syncEntireSeason(String year) {
        // Automatically default to Regular Season (2)
        syncEntireSeason(year, 2);
    }

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

    public int fetchAndSaveFromEspn(String year, int seasonType, int week) {
        int gamesSavedThisWeek = 0;
        int actualDatabaseWeek = week;
        if (seasonType == 3) { // Playoffs
            actualDatabaseWeek = week + 18; // Week 1 = Week 19
        }
        
        try {
            String url = "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard?year=" + year + "&seasontype=" + seasonType + "&week=" + week;
            String jsonResponse = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            if (root != null && root.has("events")) {
                JsonNode events = root.path("events");
                System.out.println("📡 Week " + week + " - Found " + events.size() + " games from ESPN.");
                
                for (JsonNode event : events) {
                    String gameId = event.path("id").asText("UNKNOWN_ID");
                    String status = event.path("status").path("type").path("name").asText("STATUS_UNKNOWN");
                    
                    // 🕒 Grab kickoff date/time string from ESPN (e.g. "2026-09-10T00:20Z")
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

                    // Determine winning team if game is final
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
     * Grades all user picks tied to a specific matchup once that matchup goes final.
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
}