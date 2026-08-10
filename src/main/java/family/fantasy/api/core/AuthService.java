package family.fantasy.api.core;

import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String password) {
        // Check if the username is already taken
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        // Create the new user. 
        // Note: We are using the username as the default display name for now
        User newUser = new User(username, username);
        
        // Set the password
        // IMPORTANT: In a production app, you should use BCrypt to hash this password before saving!
        newUser.setPassword(password); 
        
        return userRepository.save(newUser);
    }

    public User login(String username, String password) {
        // Find the user by username
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Invalid username or password"));
            
        // Check if the provided password matches the database
        // IMPORTANT: If you add Spring Security later, use passwordEncoder.matches() here
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        
        return user;
    }

    public User getAuthenticatedUser(String tokenUsername) {
        return userRepository.findByUsername(tokenUsername)
            .orElseThrow(() -> new RuntimeException("User not found for the provided token."));
    }
}