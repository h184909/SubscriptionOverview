package no.hvl.subscriptionapp.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "subscription_app", name = "lunchflow_connection",
        indexes = {
                @Index(name = "ix_lunchflow_conn_user", columnList = "user_email")
        }
)
public class LunchFlowConnection {

    @Id
    private UUID id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "lunchflow_user_id", length = 255)
    private String lunchflowUserId;

    @Column(name = "access_token", nullable = false, columnDefinition = "text")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    @Column(name = "institution_name", length = 255)
    private String institutionName;

    @Column(name = "account_count")
    private Integer accountCount;

    @Column(name = "account_names", columnDefinition = "text")
    private String accountNames;

    @Column(name = "connected_at", nullable = false)
    private OffsetDateTime connectedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    protected LunchFlowConnection() {}

    public LunchFlowConnection(String userEmail, String lunchflowUserId, String accessToken, String refreshToken) {
        this.id = UUID.randomUUID();
        this.userEmail = userEmail;
        this.lunchflowUserId = lunchflowUserId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.connectedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getLunchflowUserId() { return lunchflowUserId; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getInstitutionName() { return institutionName; }
    public Integer getAccountCount() { return accountCount; }
    public String getAccountNames() { return accountNames; }
    public OffsetDateTime getConnectedAt() { return connectedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getLastSyncedAt() { return lastSyncedAt; }

    public void updateTokens(String lunchflowUserId, String accessToken, String refreshToken) {
        this.lunchflowUserId = lunchflowUserId;
        this.accessToken = accessToken;

        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }

        this.updatedAt = OffsetDateTime.now();
    }

    public void markSynced(String institutionName, int accountCount, String accountNames) {
        this.institutionName = institutionName;
        this.accountCount = accountCount;
        this.accountNames = accountNames;
        this.lastSyncedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }
}