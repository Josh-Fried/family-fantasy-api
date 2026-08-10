package family.fantasy.api.locks;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScoringService {

    private final PickRepository pickRepository;
    private final PickEntryRepository pickEntryRepository;

    public ScoringService(PickRepository pickRepository, PickEntryRepository pickEntryRepository) {
        this.pickRepository = pickRepository;
        this.pickEntryRepository = pickEntryRepository;
    }

    public void gradeWeeklyPicks(Long pickEntryId, Integer weekNumber) {
        List<Pick> weeklyPicks = pickRepository.findByPickEntryIdAndMatchupWeekNumber(pickEntryId, weekNumber);
        
        if (weeklyPicks.isEmpty()) {
            return;
        }

        int correctCount = 0;
        int completedGames = 0;
        boolean hasLoss = false;
        
        for (Pick pick : weeklyPicks) {
            String actualWinner = pick.getMatchup().getWinningTeam();
            
            if (actualWinner != null) {
                completedGames++;
                
                boolean isCorrect = pick.getSelectedTeam().equals(actualWinner);
                pick.setIsCorrect(isCorrect);
                pickRepository.save(pick);
                
                if (isCorrect) {
                    correctCount++;
                } else {
                    hasLoss = true;
                }
            }
        }
        
        if (completedGames == weeklyPicks.size() && !hasLoss) {
            PickEntry entry = pickEntryRepository.findById(pickEntryId)
                .orElseThrow(() -> new RuntimeException("Entry not found"));
            entry.setTotalScore(entry.getTotalScore() + weeklyPicks.size());
            pickEntryRepository.save(entry);
        }
    }
}