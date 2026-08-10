package family.fantasy.api.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a registered user in the application.
 * This entity stores the core authentication identity, display name, 
 * and a global score used for tracking overall performance across all games.
 */
@Entity
@Table(name = "users", schema = "core_schema")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private Integer globalScore = 0;

    @Column
    private String password;

    public User() {
    }

    public User(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getGlobalScore() {
        return globalScore;
    }

    public void setGlobalScore(Integer globalScore) {
        this.globalScore = globalScore;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}