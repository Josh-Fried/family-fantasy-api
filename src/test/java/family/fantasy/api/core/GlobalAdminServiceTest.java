package family.fantasy.api.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.server.ResponseStatusException;

import family.fantasy.api.locks.MatchupRepository;
import family.fantasy.api.locks.Pick;
import family.fantasy.api.locks.PickRepository;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GlobalAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NflSyncService nflSyncService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private PickRepository pickRepository;

    @Mock
    private MatchupRepository matchupRepository;

    @Mock
    private Cache cache;

    @InjectMocks
    private GlobalAdminService globalAdminService;

    private User createAdminUser() {
        User user = new User("admin@email.com", "Admin");
        user.setGlobalAdmin(true);
        return user;
    }

    private User createRegularUser() {
        return new User("user@email.com", "User");
    }

    @Test
    void verifyGlobalAdmin_NullId_ThrowsUnauthorized() {
        assertThrows(ResponseStatusException.class, () -> globalAdminService.verifyGlobalAdmin(null));
    }

    @Test
    void verifyGlobalAdmin_UserNotFound_ThrowsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> globalAdminService.verifyGlobalAdmin(1L));
    }

    @Test
    void verifyGlobalAdmin_NotGlobalAdmin_ThrowsForbidden() {
        User regularUser = createRegularUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        assertThrows(ResponseStatusException.class, () -> globalAdminService.verifyGlobalAdmin(1L));
    }

    @Test
    void syncSeasonWithType_ValidAdmin_CallsSync() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        globalAdminService.syncSeasonWithType(1L, "2026", 2);

        verify(nflSyncService, times(1)).syncEntireSeason("2026", 2);
    }

    @Test
    void forceSyncWeek_ValidAdmin_ReturnsCount() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(nflSyncService.fetchAndSaveFromEspn("2026", 2, 5)).thenReturn(16);

        int count = globalAdminService.forceSyncWeek(1L, "2026", 2, 5);

        assertEquals(16, count);
        verify(nflSyncService, times(1)).fetchAndSaveFromEspn("2026", 2, 5);
    }

    @Test
    void forceLiveMatchupSync_RegularWeek_CallsEspnSync() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(nflSyncService.fetchAndSaveFromEspn("2026", 2, 10)).thenReturn(16);

        int count = globalAdminService.forceLiveMatchupSync(1L, "2026", 10);

        assertEquals(16, count);
        verify(nflSyncService, times(1)).fetchAndSaveFromEspn("2026", 2, 10);
    }

    @Test
    void forceLiveMatchupSync_PostseasonWeek_CalculatesCorrectSeasonTypeAndWeek() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(nflSyncService.fetchAndSaveFromEspn("2026", 3, 2)).thenReturn(4);

        int count = globalAdminService.forceLiveMatchupSync(1L, "2026", 20);

        assertEquals(4, count);
        verify(nflSyncService, times(1)).fetchAndSaveFromEspn("2026", 3, 2);
    }

    @Test
    void clearAllCaches_ValidAdmin_ClearsCaches() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(cacheManager.getCacheNames()).thenReturn(Collections.singletonList("testCache"));
        when(cacheManager.getCache("testCache")).thenReturn(cache);

        globalAdminService.clearAllCaches(1L);

        verify(cache, times(1)).clear();
    }

    @Test
    void recalculateWeekScoring_ValidAdmin_ReturnsResultMap() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        Map<String, Object> result = globalAdminService.recalculateWeekScoring(1L, 2026, 5);

        assertNotNull(result);
        assertEquals(2026, result.get("season"));
        assertEquals(5, result.get("week"));
        assertEquals("Scoring successfully recalculated.", result.get("status"));
    }

    @Test
    void overridePickSelection_ValidPick_UpdatesAndSaves() {
        User adminUser = createAdminUser();
        Pick pick = new Pick();
        pick.setSelectedTeam("OldTeam");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(pickRepository.findById(10L)).thenReturn(Optional.of(pick));
        when(pickRepository.save(any(Pick.class))).thenReturn(pick);

        Pick updated = globalAdminService.overridePickSelection(1L, 10L, "NewTeam");

        assertEquals("NewTeam", updated.getSelectedTeam());
        verify(pickRepository, times(1)).save(pick);
    }

    @Test
    void overridePickSelection_NotFound_ThrowsException() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(pickRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            globalAdminService.overridePickSelection(1L, 10L, "NewTeam");
        });

        verify(pickRepository, never()).save(any(Pick.class));
    }

    @Test
    void clearTestData_ValidAdmin_DeletesAllRecords() {
        User adminUser = createAdminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        globalAdminService.clearTestData(1L);

        verify(pickRepository, times(1)).deleteAll();
        verify(matchupRepository, times(1)).deleteAll();
    }
}