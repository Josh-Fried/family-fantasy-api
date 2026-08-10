package family.fantasy.api.locks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PickEntryRepository pickEntryRepository;

    @MockitoBean
    private PickRepository pickRepository;

    @Test
    void overrideScore_ValidRequest_ReturnsOk() throws Exception {
        PickEntry updatedEntry = new PickEntry();
        updatedEntry.setTotalScore(15);
        when(pickEntryRepository.findById(anyLong())).thenReturn(java.util.Optional.of(updatedEntry));
        when(pickEntryRepository.save(updatedEntry)).thenReturn(updatedEntry);

        mockMvc.perform(put("/api/v1/admin/entries/1/override-score")
                .param("newScore", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}