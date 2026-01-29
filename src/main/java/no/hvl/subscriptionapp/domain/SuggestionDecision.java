package no.hvl.subscriptionapp.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "suggestion_decision",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_sugg_dec_user_key", columnNames = {"user_email", "suggestion_key"})
        }
)
public class SuggestionDecision {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_email", nullable = false, length = 200)
    private String userEmail;

    @Column(name = "suggestion_key", nullable = false, length = 200)
    private String suggestionKey;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision; // ACCEPTED / REJECTED

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SuggestionDecision() {}

    public SuggestionDecision(String userEmail, String suggestionKey, String decision) {
        this.id = UUID.randomUUID();
        this.userEmail = userEmail;
        this.suggestionKey = suggestionKey;
        this.decision = decision;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getSuggestionKey() { return suggestionKey; }
    public String getDecision() { return decision; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public boolean isAccepted() { return "ACCEPTED".equalsIgnoreCase(decision); }
    public boolean isRejected() { return "REJECTED".equalsIgnoreCase(decision); }
}
