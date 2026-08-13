package family.fantasy.api.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PickRepository extends JpaRepository<Pick, Long> {
    List<Pick> findByMatchupId(Long matchupId);
    Optional<Pick> findByUserIdAndMatchupId(Long userId, Long matchupId);
    List<Pick> findByUserId(Long userId);
    List<Pick> findByUserIdAndMatchupSeasonAndMatchupWeekNumber(Long userId, Integer season, Integer weekNumber);
}