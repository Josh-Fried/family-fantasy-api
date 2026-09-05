package family.fantasy.api.locks;

import family.fantasy.api.core.User;
import jakarta.persistence.*;

/**
 * Entity representing a user's summarized fantasy score for a specific NFL week.
 * Instead of calculating points on the fly by scanning hundreds of individual game picks
 * every time the leaderboard is viewed, this entity stores the finalized score of a week.
 * This flattens the calculation to a simple O(W) sum aggregation where W is the number of weeks.
 */
@Entity
@Table(name = "user_weekly_scores", schema = "locks_schema", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "season", "week_number"})
})
public class UserWeeklyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "season")
    private int season;

    @Column(name = "week_number")
    private int weekNumber;

    @Column(name = "points_earned")
    private int pointsEarned; 

    @Column(name = "is_perfect_week")
    private boolean perfectWeek;

    public UserWeeklyScore() {}

    public UserWeeklyScore(User user, int season, int weekNumber) {
        this.user = user;
        this.season = season;
        this.weekNumber = weekNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }

    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public boolean isPerfectWeek() { return perfectWeek; }
    public void setPerfectWeek(boolean perfectWeek) { this.perfectWeek = perfectWeek; }
}