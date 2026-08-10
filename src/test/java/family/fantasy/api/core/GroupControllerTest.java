package family.fantasy.api.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
public class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private AuthService authService;

    @Test
    void createGroup_ValidRequest_ReturnsCreated() throws Exception {
        User mockUser = new User("test@email.com", "Admin");
        Group mockGroup = new Group("The Family League");

        when(authService.getAuthenticatedUser(anyString())).thenReturn(mockUser);
        when(groupService.createGroup(any(User.class), anyString(), anyString())).thenReturn(mockGroup);

        mockMvc.perform(post("/api/v1/groups")
                .param("tokenUsername", "test@email.com")
                .param("groupName", "The Family League")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }
}