package family.fantasy.api.locks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "picks", schema = "pick_em_schema")
public class Pick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "entry_id", nullable = false)
    private PickEntry pickEntry;

    @ManyToOne
    @JoinColumn(name = "matchup_id", nullable = false)
    private Matchup matchup;

    @Column(nullable = false)
    private String selectedTeam;

    @Column
    private Boolean isCorrect;

    public Pick() {
    }

    public Pick(PickEntry pickEntry, Matchup matchup, String selectedTeam) {
        this.pickEntry = pickEntry;
        this.matchup = matchup;
        this.selectedTeam = selectedTeam;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PickEntry getPickEntry() {
        return pickEntry;
    }

    public void setPickEntry(PickEntry pickEntry) {
        this.pickEntry = pickEntry;
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