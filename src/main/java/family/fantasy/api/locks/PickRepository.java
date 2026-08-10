package family.fantasy.api.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PickRepository extends JpaRepository<Pick, Long> {
    List<Pick> findByPickEntryId(Long pickEntryId);
    List<Pick> findByPickEntryIdAndMatchupWeekNumber(Long pickEntryId, Integer weekNumber);
}