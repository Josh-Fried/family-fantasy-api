package family.fantasy.api.core;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @CacheEvict(value = "groupLeaderboards", allEntries = true)
    public User updateDisplayName(Long userId, String newDisplayName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setDisplayName(newDisplayName);
        return userRepository.save(user);
    }
}