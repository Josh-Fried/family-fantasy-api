package family.fantasy.api.locks;

import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;

@Service
public class PickService {

    private final PickRepository pickRepository;
    private final MatchupRepository matchupRepository;

    public PickService(PickRepository pickRepository, MatchupRepository matchupRepository) {
        this.pickRepository = pickRepository;
        this.matchupRepository = matchupRepository;
    }

    public Pick submitPick(PickEntry pickEntry, Long matchupId, String selectedTeam) {
        Matchup matchup = matchupRepository.findById(matchupId)
            .orElseThrow(() -> new RuntimeException("Matchup not found"));
        if (OffsetDateTime.now().isAfter(matchup.getKickoffTime())) {
            throw new RuntimeException("Too late to pick. The game has already kicked off.");
        }
        Pick pick = new Pick(pickEntry, matchup, selectedTeam);
        return pickRepository.save(pick);
    }
}