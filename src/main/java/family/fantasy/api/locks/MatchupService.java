package family.fantasy.api.locks;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MatchupService {

    private final MatchupRepository matchupRepository;

    public MatchupService(MatchupRepository matchupRepository) {
        this.matchupRepository = matchupRepository;
    }

    public List<Matchup> getWeeklyMatchups(Integer season, Integer weekNumber) {
        return matchupRepository.findBySeasonAndWeekNumber(season, weekNumber);
    }

    public Matchup setMatchupResult(Long matchupId, String winningTeam) {
        Matchup matchup = matchupRepository.findById(matchupId)
            .orElseThrow(() -> new RuntimeException("Matchup not found"));
        matchup.setWinningTeam(winningTeam);
        return matchupRepository.save(matchup);
    }
}