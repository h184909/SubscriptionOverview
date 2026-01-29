package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.service.ImportStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ImportStatusController {

    private final ImportStatusService importStatus;

    public ImportStatusController(ImportStatusService importStatus) {
        this.importStatus = importStatus;
    }

    @GetMapping("/app/import-status")
    public Map<String, Object> status(HttpSession session) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) {
            return Map.of("state", "IDLE", "message", "Ikke innlogget");
        }

        var s = importStatus.get(email);
        return Map.of(
                "state", s.getState().name(),
                "message", s.getMessage(),
                "updatedAt", String.valueOf(s.getUpdatedAt())
        );
    }
}
