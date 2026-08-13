package family.fantasy.api.core;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final NflStateService nflStateService;

    public SystemController(NflStateService nflStateService) {
        this.nflStateService = nflStateService;
    }

    @GetMapping("/season/{season}/current-week")
    public ResponseEntity<Integer> getCurrentWeek(@PathVariable int season) {
        return ResponseEntity.ok(nflStateService.getCurrentWeek());
    }

    @GetMapping("/current-season")
    public ResponseEntity<Integer> getCurrentSeason() {
        return ResponseEntity.ok(nflStateService.getCurrentSeason());
    }
}