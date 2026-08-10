package family.fantasy.api.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    /**
     * Tests that a user's display name is successfully updated and saved 
     * when a valid user ID is provided.
     */
    @Test
    void updateDisplayName_ValidUser_UpdatesAndSaves() {
        User mockUser = new User("user@test.com", "OldName");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(mockUser)).thenReturn(mockUser);

        User result = userService.updateDisplayName(1L, "NewName");

        assertEquals("NewName", result.getDisplayName());
        verify(userRepository, times(1)).save(mockUser);
    }

    /**
     * Tests that a RuntimeException is thrown if the user ID does not exist 
     * in the database, preventing null pointer errors.
     */
    @Test
    void updateDisplayName_InvalidUser_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            userService.updateDisplayName(99L, "NewName");
        });
    }
}