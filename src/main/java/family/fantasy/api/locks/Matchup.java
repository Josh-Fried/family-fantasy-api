package family.fantasy.api.locks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "matchups", schema = "pick_em_schema")
public class Matchup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer season;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private String awayTeam;

    @Column(nullable = false)
    private String homeTeam;

    @Column(nullable = false)
    private LocalDateTime kickoffTime;

    @Column
    private String winningTeam;

    public Matchup() {
    }

    public Matchup(Integer season, Integer weekNumber, String awayTeam, String homeTeam, LocalDateTime kickoffTime) {
        this.season = season;
        this.weekNumber = weekNumber;
        this.awayTeam = awayTeam;
        this.homeTeam = homeTeam;
        this.kickoffTime = kickoffTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    public LocalDateTime getKickoffTime() {
        return kickoffTime;
    }

    public void setKickoffTime(LocalDateTime kickoffTime) {
        this.kickoffTime = kickoffTime;
    }

    public String getWinningTeam() {
        return winningTeam;
    }

    public void setWinningTeam(String winningTeam) {
        this.winningTeam = winningTeam;
    }
}