package family.fantasy.api.locks;

import family.fantasy.api.core.Group;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Resolves the many-to-many relationship between PickEntry and Group.
 * Allows a single PickEntry to compete across one or more family groups.
 */
@Entity
@Table(name = "group_entries", schema = "pick_em_schema")
public class GroupEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "entry_id", nullable = false)
    private PickEntry pickEntry;

    public GroupEntry() {
    }

    public GroupEntry(Group group, PickEntry pickEntry) {
        this.group = group;
        this.pickEntry = pickEntry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public PickEntry getPickEntry() {
        return pickEntry;
    }

    public void setPickEntry(PickEntry pickEntry) {
        this.pickEntry = pickEntry;
    }
}