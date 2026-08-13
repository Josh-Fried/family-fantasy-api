package family.fantasy.api.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import family.fantasy.api.core.Group.GameType;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    List<UserGroup> findByUserId(Long userId);
    List<UserGroup> findByGroupId(Long groupId);
    @Query("SELECT COUNT(ug) FROM UserGroup ug WHERE ug.user.id = :userId AND ug.group.gameType = :gameType AND ug.isAdmin = true")
    long countActiveGroupsForUser(@Param("userId") Long userId, @Param("gameType") GameType gameType);
}