package family.fantasy.api.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NflStateService {

    private final RestTemplate restTemplate;

    public NflStateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches current state from Sleeper and caches the composite object.
     * Evicted on Tuesdays via NflSyncService.
     */
    @Value("${fantasy.testing.preseason-override:false}")
    private boolean preseasonOverride;

    @Value("${fantasy.testing.override-week:1}")
    private int overrideWeek;

    @Cacheable("nflState")
    public NflState getNflState() {
        if (preseasonOverride) {
            return new NflState(2026, overrideWeek, "pre");
        }
        
        try {
            String sleeperUrl = "https://api.sleeper.app/v1/state/nfl";
            SleeperStateResponse response = restTemplate.getForObject(sleeperUrl, SleeperStateResponse.class);

            if (response != null) {
                int week = response.week() > 0 ? response.week() : 1;
                int season = response.season() > 0 ? response.season() : 2026;
                String seasonType = response.seasonType() != null ? response.seasonType() : "regular";
                
                return new NflState(season, week, seasonType);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch state from Sleeper: " + e.getMessage());
        }

        return new NflState(2026, 1, "regular");
    }

    public int getCurrentWeek() {
        return getNflState().week();
    }

    public int getCurrentSeason() {
        return getNflState().season();
    }

    // Main Service DTO
    public record NflState(int season, int week, String seasonType) {}

    // Helper Record to map Sleeper's JSON fields directly
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SleeperStateResponse(
        int season,
        int week,
        @JsonProperty("season_type") String seasonType
    ) {}
}