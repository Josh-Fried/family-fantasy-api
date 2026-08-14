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
    private OffsetDateTime getSunday11AmMst(Matchup matchup) {
        // 1. Convert kickoff to Mountain Time safely (handles Daylight Saving automatically)
        java.time.ZonedDateTime mountainKickoff = matchup.getKickoffTime()
                .atZoneSameInstant(java.time.ZoneId.of("America/Denver"));
        
        java.time.ZonedDateTime targetSunday;

        // 2. NFL weeks run Thursday -> Monday. 
        // If the game is played Tues-Sat, the main Sunday is the NEXT Sunday.
        // If the game is played on Sunday or Monday, the main Sunday is the PREVIOUS or SAME Sunday.
        switch (mountainKickoff.getDayOfWeek()) {
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
            case FRIDAY:
            case SATURDAY:
                targetSunday = mountainKickoff.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.SUNDAY));
                break;
            default: // SUNDAY or MONDAY
                targetSunday = mountainKickoff.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
                break;
        }

        // 3. Set the time to exactly 11:00 AM Mountain Time and convert back to OffsetDateTime
        return targetSunday
                .withHour(11)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toOffsetDateTime();
    }

    // Validates if a pick can be made or removed
    private void validatePickTiming(Matchup matchup) {
        System.out.println("Validating Pick Timing");
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("America/Denver"));
        
        // Rule 1: Always lock at the game's exact kickoff time (handles Thurs/Fri, and early London games)
        if (now.isAfter(matchup.getKickoffTime()) || now.isEqual(matchup.getKickoffTime())) {
            System.out.println("1");
            throw new RuntimeException("Too late to change. The game has already kicked off.");
        }

        // Rule 2: Lock everything else at Sunday 11:00 AM Mountain Time
        OffsetDateTime sunday11AM = getSunday11AmMst(matchup);
        if (now.isAfter(sunday11AM) || now.isEqual(sunday11AM)) {
            System.out.println("2");
            throw new RuntimeException("Picks are locked for the week. The Sunday 11:00 AM deadline has passed.");
        }

        System.out.println("PICK SAVED");
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

        // 2. Fetch the matchups for this specific week
        List<Matchup> weekMatchups = matchupRepository.findBySeasonAndWeekNumber(season, week);
        
        // If there are no games, there are no picks to reveal
        if (weekMatchups == null || weekMatchups.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 3. Pass ANY game from this week into our robust calculator to get the Sunday deadline
        OffsetDateTime revealTime = getSunday11AmMst(weekMatchups.get(0));

        // 4. Block the request if the current time is before Sunday at 11 AM
        if (OffsetDateTime.now(java.time.ZoneId.of("America/Denver")).isBefore(revealTime)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, 
                "Other players' picks are locked until Sunday at 11:00 AM MST."
            );
        }

        // 5. If time has passed, return the picks safely
        return pickRepository.findByUserIdAndMatchupSeasonAndMatchupWeekNumber(targetUserId, season, week);
    }
}