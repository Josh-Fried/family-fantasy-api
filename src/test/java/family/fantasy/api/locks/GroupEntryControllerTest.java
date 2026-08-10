package family.fantasy.api.locks;

import com.fasterxml.jackson.databind.ObjectMapper;
import family.fantasy.api.core.GroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupEntryController.class)
public class GroupEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupEntryRepository groupEntryRepository;

    @MockitoBean
    private GroupRepository groupRepository;

    @MockitoBean
    private PickEntryRepository pickEntryRepository;

    @Test
    void getLeaderboard_ValidGroupId_ReturnsSortedList() throws Exception {
        GroupEntry entry1 = new GroupEntry();
        GroupEntry entry2 = new GroupEntry();
        when(groupEntryRepository.findByGroupIdOrderByPickEntryTotalScoreDesc(anyLong()))
                .thenReturn(Arrays.asList(entry1, entry2));

        mockMvc.perform(get("/api/v1/group-entries/group/1/leaderboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}