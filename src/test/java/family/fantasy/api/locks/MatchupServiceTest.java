package family.fantasy.api.locks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchupServiceTest {

    @Mock
    private MatchupRepository matchupRepository;

    @InjectMocks
    private MatchupService matchupService;

    /**
     * Tests that setting a matchup result updates the winning team field 
     * and persists the changes to the database.
     */
    @Test
    void setMatchupResult_ValidMatchup_UpdatesWinningTeam() {
        Matchup mockMatchup = new Matchup(2026, 1, "Bills", "Dolphins", LocalDateTime.now());
        when(matchupRepository.findById(1L)).thenReturn(Optional.of(mockMatchup));
        when(matchupRepository.save(mockMatchup)).thenReturn(mockMatchup);

        Matchup result = matchupService.setMatchupResult(1L, "Bills");

        assertEquals("Bills", result.getWinningTeam());
        verify(matchupRepository, times(1)).save(mockMatchup);
    }
}