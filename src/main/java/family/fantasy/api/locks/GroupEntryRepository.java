package family.fantasy.api.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupEntryRepository extends JpaRepository<GroupEntry, Long> {
    List<GroupEntry> findByGroupIdOrderByPickEntryTotalScoreDesc(Long groupId);
    List<GroupEntry> findByPickEntryId(Long pickEntryId);
}