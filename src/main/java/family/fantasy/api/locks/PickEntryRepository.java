package family.fantasy.api.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PickEntryRepository extends JpaRepository<PickEntry, Long> {
    List<PickEntry> findByUserIdAndSeason(Long userId, Integer season);
}
