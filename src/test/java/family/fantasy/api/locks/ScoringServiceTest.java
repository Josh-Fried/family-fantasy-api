package family.fantasy.api.locks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoringServiceTest {

    @Mock
    private PickRepository pickRepository;

    @Mock
    private PickEntryRepository pickEntryRepository;

    @InjectMocks
    private ScoringService scoringService;

    /**
     * Tests the critical "all or nothing" logic where all three picks are correct, 
     * resulting in exactly three points being added to the user's total score.
     */
    @Test
    void gradeWeeklyPicks_ThreeCorrect_AwardsThreePoints() {
        PickEntry mockEntry = new PickEntry();
        mockEntry.setTotalScore(10);
        
        Matchup m1 = new Matchup(); m1.setWinningTeam("Bills");
        Matchup m2 = new Matchup(); m2.setWinningTeam("Patriots");
        Matchup m3 = new Matchup(); m3.setWinningTeam("Saints");

        Pick p1 = new Pick(mockEntry, m1, "Bills");
        Pick p2 = new Pick(mockEntry, m2, "Patriots");
        Pick p3 = new Pick(mockEntry, m3, "Saints");
        
        when(pickRepository.findByPickEntryIdAndMatchupWeekNumber(1L, 1))
            .thenReturn(Arrays.asList(p1, p2, p3));
        when(pickEntryRepository.findById(1L)).thenReturn(Optional.of(mockEntry));

        scoringService.gradeWeeklyPicks(1L, 1);

        assertEquals(13, mockEntry.getTotalScore());
        verify(pickEntryRepository, times(1)).save(mockEntry);
    }

    /**
     * Tests the "all or nothing" fail condition, ensuring that if even one pick 
     * out of three is incorrect, the total score remains unchanged (zero points awarded).
     */
    @Test
    void gradeWeeklyPicks_OneWrong_AwardsZeroPoints() {
        PickEntry mockEntry = new PickEntry();
        mockEntry.setTotalScore(10);
        
        Matchup m1 = new Matchup(); m1.setWinningTeam("Bills");
        Matchup m2 = new Matchup(); m2.setWinningTeam("Rams"); // Wrong pick upcoming
        Matchup m3 = new Matchup(); m3.setWinningTeam("Saints");

        Pick p1 = new Pick(mockEntry, m1, "Bills");
        Pick p2 = new Pick(mockEntry, m2, "Patriots"); // Picked Patriots, Rams won
        Pick p3 = new Pick(mockEntry, m3, "Saints");
        
        when(pickRepository.findByPickEntryIdAndMatchupWeekNumber(1L, 1))
            .thenReturn(Arrays.asList(p1, p2, p3));

        scoringService.gradeWeeklyPicks(1L, 1);

        assertEquals(10, mockEntry.getTotalScore()); // Score did not increment
        verify(pickEntryRepository, never()).save(any(PickEntry.class));
    }
}