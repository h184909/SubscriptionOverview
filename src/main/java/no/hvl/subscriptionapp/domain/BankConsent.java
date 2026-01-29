package no.hvl.subscriptionapp.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "subscription_app", name = "bank_consent")
public class BankConsent {

    @Id
    private UUID id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "institution_id", nullable = false)
    private String institutionId;

    @Column(name = "consent_token", nullable = false, columnDefinition = "text")
    private String consentToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected BankConsent() {}

    public BankConsent(String userEmail, String institutionId, String consentToken) {
        this.id = UUID.randomUUID();
        this.userEmail = userEmail;
        this.institutionId = institutionId;
        this.consentToken = consentToken;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getInstitutionId() { return institutionId; }
    public String getConsentToken() { return consentToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
