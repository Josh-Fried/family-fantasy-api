package family.fantasy.api.locks;

import family.fantasy.api.core.User;
import jakarta.persistence.*;

@Entity
@Table(name = "picks", schema = "locks_schema")
public class Pick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "matchup_id", nullable = false)
    private Matchup matchup;

    @Column(nullable = false)
    private String selectedTeam;

    private Boolean isCorrect; 

    // Constructors
    public Pick() {}

    public Pick(User user, Matchup matchup, String selectedTeam) {
        this.user = user;
        this.matchup = matchup;
        this.selectedTeam = selectedTeam;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Matchup getMatchup() {
        return matchup;
    }

    public void setMatchup(Matchup matchup) {
        this.matchup = matchup;
    }

    public String getSelectedTeam() {
        return selectedTeam;
    }

    public void setSelectedTeam(String selectedTeam) {
        this.selectedTeam = selectedTeam;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}