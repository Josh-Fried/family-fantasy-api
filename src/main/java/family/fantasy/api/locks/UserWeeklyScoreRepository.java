package family.fantasy.api.locks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository interface for managing UserWeeklyScore database operations.
 * It provides custom queries to retrieve a user's summarized week score and a highly 
 * optimized sum query that calculates total points across an entire season using COALESCE 
 * to gracefully handle users with no scored weeks.
 */
@Repository
public interface UserWeeklyScoreRepository extends JpaRepository<UserWeeklyScore, Long> {

    /**
     * Finds a specific user's summarized score for a distinct week and season.
     */
    Optional<UserWeeklyScore> findByUserIdAndSeasonAndWeekNumber(Long userId, int season, int weekNumber);

    /**
     * Sums up all weekly points earned by a specific user across a given season.
     * COALESCE is utilized to ensure a safe 0 return value instead of null if the user has no points.
     */
    @Query("SELECT COALESCE(SUM(u.pointsEarned), 0) FROM UserWeeklyScore u WHERE u.user.id = :userId AND u.season = :season")
    int findTotalScoreByUserIdAndSeason(@Param("userId") Long userId, @Param("season") int season);

    /**
     * Retrieves all weekly score summaries for a user in a given season, 
     * ordered from newest week to oldest week.
     */
    List<UserWeeklyScore> findByUserIdAndSeasonOrderByWeekNumberDesc(Long userId, int season);
}