package family.fantasy.api.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    /**
     * Tests that if a user already exists with the provided token username, 
     * the service returns that existing user without saving a new one.
     */
    @Test
    void authenticateOrRegister_ExistingUser_ReturnsUser() {
        User existingUser = new User("test@email.com", "TestUser");
        when(userRepository.findByUsername("test@email.com")).thenReturn(Optional.of(existingUser));

        User result = authService.register("test@email.com", "NewName");

        assertEquals("TestUser", result.getDisplayName());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests that if no user matches the token username, the service 
     * registers a new user with the default display name and saves them.
     */
    @Test
    void authenticateOrRegister_NewUser_SavesAndReturnsUser() {
        when(userRepository.findByUsername("new@email.com")).thenReturn(Optional.empty());
        User savedUser = new User("new@email.com", "DefaultName");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register("new@email.com", "DefaultName");

        assertEquals("DefaultName", result.getDisplayName());
        verify(userRepository, times(1)).save(any(User.class));
    }
}