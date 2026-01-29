package no.hvl.subscriptionapp.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(schema = "subscription_app", name = "bank_transaction",
        indexes = {
                @Index(name = "ix_bank_tx_user_account", columnList = "user_email, account_id"),
                @Index(name = "ix_bank_tx_date", columnList = "tx_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_bank_tx_user_account_txid",
                        columnNames = {"user_email", "account_id", "tx_id"})
        }
)
public class BankTransaction {

    @Id
    private UUID id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "account_id", nullable = false, length = 255)
    private String accountId;

    @Column(name = "tx_id", nullable = false, length = 255)
    private String txId;

    @Column(name = "tx_date")
    private OffsetDateTime txDate;

    @Column(name = "tx_date_raw", length = 80)
    private String txDateRaw;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BankTransaction() {}

    public BankTransaction(
            String userEmail,
            String accountId,
            String txId,
            OffsetDateTime txDate,
            String txDateRaw,
            String description,
            String reference,
            BigDecimal amount,
            String currency
    ) {
        this.id = UUID.randomUUID();
        this.userEmail = userEmail;
        this.accountId = accountId;
        this.txId = txId;
        this.txDate = txDate;
        this.txDateRaw = txDateRaw;
        this.description = description;
        this.reference = reference;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getAccountId() { return accountId; }
    public String getTxId() { return txId; }
    public OffsetDateTime getTxDate() { return txDate; }
    public String getTxDateRaw() { return txDateRaw; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public LocalDate getTxLocalDate() {
        if (txDate != null) return txDate.toLocalDate();
        // fallback: hvis Yapily ga oss noe rart og parse feilet
        try {
            if (txDateRaw != null && !txDateRaw.isBlank()) {
                return OffsetDateTime.parse(txDateRaw).toLocalDate();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
