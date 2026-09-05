package family.fantasy.api.locks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "matchups", schema = "locks_schema")
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
    private OffsetDateTime kickoffTime;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private Integer homeScore;

    @Column(nullable = false)
    private Integer awayScore;

    @Column(nullable = false)
    private String status;

    @Column
    private String winningTeam;

    public Matchup() {
    }

    public Matchup(Integer season, Integer weekNumber, String awayTeam, String homeTeam, OffsetDateTime kickoffTime, Integer homeScore, Integer awayScore, String externalId, String status) {
        this.season = season;
        this.weekNumber = weekNumber;
        this.awayTeam = awayTeam;
        this.homeTeam = homeTeam;
        this.kickoffTime = kickoffTime;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.externalId = externalId;
        this.status = status;
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

    public OffsetDateTime getKickoffTime() {
        return kickoffTime;
    }

    public void setKickoffTime(OffsetDateTime kickoffTime) {
        this.kickoffTime = kickoffTime;
    }

    public String getExternalId() {
        return winningTeam;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public String getWinningTeam() {
        return winningTeam;
    }

    public void setWinningTeam(String winningTeam) {
        this.winningTeam = winningTeam;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}