package family.fantasy.api.locks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PickController.class)
public class PickControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchupService matchupService;

    @MockitoBean
    private PickService pickService;

    @MockitoBean
    private PickEntryRepository pickEntryRepository;

    @Test
    void submitPick_ValidRequest_ReturnsCreated() throws Exception {
        PickEntry mockEntry = new PickEntry();
        Pick mockPick = new Pick();
        mockPick.setSelectedTeam("Bills");

        when(pickEntryRepository.findById(anyLong())).thenReturn(java.util.Optional.of(mockEntry));
        when(pickService.submitPick(any(), anyLong(), anyString())).thenReturn(mockPick);

        mockMvc.perform(post("/api/v1/locks/entry/1/matchup/1")
                .param("selectedTeam", "Bills")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }
}