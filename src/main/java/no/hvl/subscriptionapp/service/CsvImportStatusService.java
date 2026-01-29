package no.hvl.subscriptionapp.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CsvImportStatusService {

    public enum State { IDLE, RUNNING, DONE, FAILED }

    public record Status(State state, String message, OffsetDateTime updatedAt) {}

    private final Map<String, Status> map = new ConcurrentHashMap<>();

    public Status get(String userEmail) {
        return map.getOrDefault(userEmail, new Status(State.IDLE, "", OffsetDateTime.now()));
    }

    public void set(String userEmail, State state, String message) {
        map.put(userEmail, new Status(state, message, OffsetDateTime.now()));
    }
}
