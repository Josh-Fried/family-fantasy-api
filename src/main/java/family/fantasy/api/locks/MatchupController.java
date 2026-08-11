package family.fantasy.api.locks;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matchups")
public class MatchupController {

    private final MatchupRepository matchupRepository;

    public MatchupController(MatchupRepository matchupRepository) {
        this.matchupRepository = matchupRepository;
    }

    @GetMapping("/season/{season}/week/{week}")
    public ResponseEntity<List<Matchup>> getMatchupsForWeek(
            @PathVariable Integer season, 
            @PathVariable Integer week) {
            
        // Fetches the games sorted perfectly from earliest to latest
        List<Matchup> matchups = matchupRepository.findBySeasonAndWeekNumberOrderByKickoffTimeAsc(season, week);
        
        return ResponseEntity.ok(matchups);
    }
}