package family.fantasy.api.locks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PickServiceTest {

    @Mock
    private PickRepository pickRepository;

    @Mock
    private MatchupRepository matchupRepository;

    @InjectMocks
    private PickService pickService;

    /**
     * Tests that a pick is successfully saved if the current system time 
     * is before the matchup's designated kickoff time.
     */
    @Test
    void submitPick_BeforeKickoff_SavesPick() {
        PickEntry entry = new PickEntry();
        Matchup futureMatchup = new Matchup(2026, 1, "Bills", "Dolphins", LocalDateTime.now().plusDays(1));
        when(matchupRepository.findById(1L)).thenReturn(Optional.of(futureMatchup));
        
        pickService.submitPick(entry, 1L, "Bills");

        verify(pickRepository, times(1)).save(any(Pick.class));
    }

    /**
     * Tests the strict deadline validation, ensuring a RuntimeException is thrown 
     * and no pick is saved if the user submits after kickoff.
     */
    @Test
    void submitPick_AfterKickoff_ThrowsException() {
        PickEntry entry = new PickEntry();
        Matchup pastMatchup = new Matchup(2026, 1, "Bills", "Dolphins", LocalDateTime.now().minusHours(1));
        when(matchupRepository.findById(1L)).thenReturn(Optional.of(pastMatchup));

        assertThrows(RuntimeException.class, () -> {
            pickService.submitPick(entry, 1L, "Bills");
        });
        verify(pickRepository, never()).save(any(Pick.class));
    }
}