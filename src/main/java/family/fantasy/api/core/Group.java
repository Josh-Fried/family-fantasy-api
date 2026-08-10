package family.fantasy.api.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Represents a league or group that users can join to compete against each other.
 * This entity manages the group's display name and the unique alphanumeric 
 * invite code required for new family members to gain access.
 */
@Entity
@Table(name = "groups", schema = "core_schema")
public class Group {

    // The nested Enum definition
    public enum GameType {
        LOCKS,
        GENERAL,
        G,
        B;
        
        public static GameType fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                return G;
            }
            try {
                // System.out.println("fromString: " + GameType.valueOf(value.trim().toUpperCase().replace(" ", "_")));
                return GameType.valueOf(value.trim().toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                // System.out.println("Unknown game type provided, defaulting to GENERAL: " + value);
                return B;
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column
    private GameType gameType;

    public Group() {
    }

    public Group(String name) {
        this.name = name;
        this.inviteCode = UUID.randomUUID().toString().substring(0, 8);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public GameType getGameType() {
        // System.out.println("GET: " + gameType);
        return gameType;
    }

    public void setGameType(GameType gameType) {
        // System.out.println("SET: " + gameType);
        this.gameType = gameType;
    }
}