package no.hvl.subscriptionapp.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImportStatusService {

    public enum State { IDLE, RUNNING, DONE, FAILED }

    public static class Status {
        private final State state;
        private final String message;
        private final OffsetDateTime updatedAt;

        public Status(State state, String message, OffsetDateTime updatedAt) {
            this.state = state;
            this.message = message;
            this.updatedAt = updatedAt;
        }

        public State getState() { return state; }
        public String getMessage() { return message; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
    }

    // per bruker
    private final Map<String, Status> byUser = new ConcurrentHashMap<>();

    public Status get(String userEmail) {
        return byUser.getOrDefault(userEmail, new Status(State.IDLE, "", OffsetDateTime.now()));
    }

    public void set(String userEmail, State state, String message) {
        byUser.put(userEmail, new Status(state, message == null ? "" : message, OffsetDateTime.now()));
    }

    public void clearDoneToIdleAfter(String userEmail, int seconds) {
        // valgfritt: hvis du vil auto-rydde DONE senere (ikke nødvendig nå)
    }
}
