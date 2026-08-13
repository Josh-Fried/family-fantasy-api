package family.fantasy.api.locks;

import family.fantasy.api.core.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PickService {

    private final PickRepository pickRepository;
    private final MatchupRepository matchupRepository;

    public PickService(PickRepository pickRepository, MatchupRepository matchupRepository) {
        this.pickRepository = pickRepository;
        this.matchupRepository = matchupRepository;
    }

    // Helper method to find exactly Sunday at 11:00 AM MST for the given week
    private OffsetDateTime getSunday11AmMst(int season, int weekNumber) {
        List<Matchup> weeklyMatchups = matchupRepository.findBySeasonAndWeekNumber(season, weekNumber);

        // Find a Sunday game to get the correct date
        Optional<Matchup> sundayGame = weeklyMatchups.stream()
                .filter(m -> m.getKickoffTime().atZoneSameInstant(ZoneId.of("America/Denver")).getDayOfWeek() == DayOfWeek.SUNDAY)
                .findFirst();

        if (sundayGame.isPresent()) {
            // Extract the date of that Sunday, and force the time to 11:00 AM MST
            return sundayGame.get().getKickoffTime()
                    .atZoneSameInstant(ZoneId.of("America/Denver"))
                    .toLocalDate() // Gets just the YYYY-MM-DD
                    .atTime(11, 0) // Sets time to 11:00 AM
                    .atZone(ZoneId.of("America/Denver"))
                    .toOffsetDateTime();
        } else {
            // Fallback: If there are literally no Sunday games this week, fallback to the very first game
            return weeklyMatchups.stream()
                    .min(Comparator.comparing(Matchup::getKickoffTime))
                    .map(Matchup::getKickoffTime)
                    .orElse(OffsetDateTime.MAX);
        }
    }

    // Validates if a pick can be made or removed
    private void validatePickTiming(Matchup matchup) {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("America/Denver"));
        
        // Rule 1: Always lock at the game's exact kickoff time (handles Thurs/Fri, and early London games)
        if (now.isAfter(matchup.getKickoffTime()) || now.isEqual(matchup.getKickoffTime())) {
            throw new RuntimeException("Too late to change. The game has already kicked off.");
        }

        // Rule 2: Lock everything else at Sunday 11:00 AM MST
        OffsetDateTime sunday11AM = getSunday11AmMst(matchup.getSeason(), matchup.getWeekNumber());
        if (now.isAfter(sunday11AM) || now.isEqual(sunday11AM)) {
            throw new RuntimeException("Picks are locked for the week. The Sunday 11:00 AM MST deadline has passed.");
        }
    }

    public Pick submitPick(User user, Long matchupId, String selectedTeam) {
        Matchup matchup = matchupRepository.findById(matchupId)
                .orElseThrow(() -> new RuntimeException("Matchup not found"));

        validatePickTiming(matchup);

        Optional<Pick> existingPick = pickRepository.findByUserIdAndMatchupId(user.getId(), matchupId);

        Pick pickToSave;
        if (existingPick.isPresent()) {
            pickToSave = existingPick.get();
            pickToSave.setSelectedTeam(selectedTeam);
        } else {
            pickToSave = new Pick();
            pickToSave.setUser(user);
            pickToSave.setMatchup(matchup);
            pickToSave.setSelectedTeam(selectedTeam);
        }

        return pickRepository.save(pickToSave);
    }

    public void removePick(Long userId, Long matchupId) {
        Matchup matchup = matchupRepository.findById(matchupId)
                .orElseThrow(() -> new RuntimeException("Matchup not found"));

        validatePickTiming(matchup);

        pickRepository.findByUserIdAndMatchupId(userId, matchupId)
                .ifPresent(pickRepository::delete);
    }

    public List<Pick> getPicksSecurely(Long targetUserId, Long requestingUserId, int season, int week) {
        // 1. You can ALWAYS see your own picks
        if (targetUserId.equals(requestingUserId)) {
            return pickRepository.findByUserIdAndMatchupSeasonAndMatchupWeekNumber(targetUserId, season, week);
        }

        // 2. Fetch exactly Sunday at 11:00 AM MST
        OffsetDateTime revealTime = getSunday11AmMst(season, week);

        // 3. Block the request if the current time is before Sunday at 11 AM
        if (OffsetDateTime.now(ZoneId.of("America/Denver")).isBefore(revealTime)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Other players' picks are locked until Sunday at 11:00 AM MST.");
        }

        // 4. If time has passed, return the picks safely
        return pickRepository.findByUserIdAndMatchupSeasonAndMatchupWeekNumber(targetUserId, season, week);
    }
}