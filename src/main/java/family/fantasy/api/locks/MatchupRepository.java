package family.fantasy.api.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchupRepository extends JpaRepository<Matchup, Long> {
    List<Matchup> findBySeasonAndWeekNumber(Integer season, Integer weekNumber);
}