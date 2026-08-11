package family.fantasy.api.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchupRepository extends JpaRepository<Matchup, Long> {
    List<Matchup> findBySeasonAndWeekNumber(Integer season, Integer weekNumber);
    Optional<Matchup> findByExternalId(String externalId);
    List<Matchup> findBySeasonAndWeekNumberOrderByKickoffTimeAsc(Integer season, Integer weekNumber);
}