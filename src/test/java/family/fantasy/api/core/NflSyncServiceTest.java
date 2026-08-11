package family.fantasy.api.core;

import family.fantasy.api.locks.Matchup;
import family.fantasy.api.locks.MatchupRepository;
import family.fantasy.api.locks.Pick;
import family.fantasy.api.locks.PickRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NflSyncServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MatchupRepository matchupRepository;

    @Mock
    private PickRepository pickRepository;

    @InjectMocks
    private NflSyncService nflSyncService;

    // A tiny, minified version of the ESPN JSON for one completed game
    private final String mockEspnJsonResponse = """
            {
              "events": [
                {
                  "id": "401872656",
                  "date": "2026-09-10T00:20Z",
                  "status": { "type": { "name": "STATUS_FINAL" } },
                  "competitions": [
                    {
                      "competitors": [
                        { "homeAway": "home", "team": { "displayName": "Seattle Seahawks" }, "score": 24 },
                        { "homeAway": "away", "team": { "displayName": "New England Patriots" }, "score": 17 }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

    @Test
    void fetchAndSaveFromEspn_GameGoesFinal_TriggersGrading() {
        // 1. ARRANGE: Set up our fake database state
        
        // The game in our DB is currently SCHEDULED (0-0)
        Matchup existingMatchup = new Matchup();
        existingMatchup.setId(1L);
        existingMatchup.setExternalId("401872656");
        existingMatchup.setHomeTeam("Seattle Seahawks");
        existingMatchup.setAwayTeam("New England Patriots");
        existingMatchup.setStatus("STATUS_SCHEDULED");
        
        // User A picked the Seahawks (The Winner)
        Pick correctPick = new Pick();
        correctPick.setId(101L);
        correctPick.setSelectedTeam("Seattle Seahawks");
        correctPick.setIsCorrect(null); // Not graded yet

        // User B picked the Patriots (The Loser)
        Pick incorrectPick = new Pick();
        incorrectPick.setId(102L);
        incorrectPick.setSelectedTeam("New England Patriots");
        incorrectPick.setIsCorrect(null); // Not graded yet

        // Tell our Mocks how to behave when the Service calls them
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockEspnJsonResponse);
        when(matchupRepository.findByExternalId("401872656")).thenReturn(Optional.of(existingMatchup));
        when(pickRepository.findByMatchupId(1L)).thenReturn(List.of(correctPick, incorrectPick));

        // 2. ACT: Run the sync method (simulating a Sunday afternoon update)
        nflSyncService.fetchAndSaveFromEspn("2026", 2, 1);

        // 3. ASSERT: Verify the logic worked exactly as expected
        
        // Capture the matchup that was saved to ensure scores and status updated
        ArgumentCaptor<Matchup> matchupCaptor = ArgumentCaptor.forClass(Matchup.class);
        verify(matchupRepository, times(1)).save(matchupCaptor.capture());
        
        Matchup savedMatchup = matchupCaptor.getValue();
        assertEquals("STATUS_FINAL", savedMatchup.getStatus(), "Game should be updated to FINAL");
        assertEquals(24, savedMatchup.getHomeScore(), "Home score should be 24");
        assertEquals(17, savedMatchup.getAwayScore(), "Away score should be 17");
        assertEquals("Seattle Seahawks", savedMatchup.getWinningTeam(), "Seahawks should be determined the winner");

        // Verify that grading was triggered and picks were updated
        ArgumentCaptor<Pick> pickCaptor = ArgumentCaptor.forClass(Pick.class);
        verify(pickRepository, times(2)).save(pickCaptor.capture());
        
        List<Pick> savedPicks = pickCaptor.getAllValues();
        
        // Find our specific picks in the captured saves
        Pick gradedCorrectPick = savedPicks.stream().filter(p -> p.getId() == 101L).findFirst().get();
        Pick gradedIncorrectPick = savedPicks.stream().filter(p -> p.getId() == 102L).findFirst().get();

        assertTrue(gradedCorrectPick.getIsCorrect(), "User A's pick should be marked correct (true)");
        assertFalse(gradedIncorrectPick.getIsCorrect(), "User B's pick should be marked incorrect (false)");
    }
}