package no.hvl.subscriptionapp.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "subscription_app", name = "subscription")
public class Subscription {

    @Id
    private UUID id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "NOK";

    @Column(nullable = false, length = 10)
    private String interval;

    @Column(name = "next_charge_date")
    private LocalDate nextChargeDate;

    @Column(name = "billing_email")
    private String billingEmail;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ✅ NYTT
    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    // ✅ NYTT
    @Column(name = "provider_key", length = 80)
    private String providerKey;

    protected Subscription() {}

    public Subscription(
            String userEmail,
            String name,
            BigDecimal amount,
            String currency,
            String interval,
            LocalDate nextChargeDate,
            String billingEmail
    ) {
        this.id = UUID.randomUUID();
        this.userEmail = userEmail;
        this.name = name;
        this.amount = amount;
        this.currency = (currency == null || currency.isBlank()) ? "NOK" : currency.trim().toUpperCase();
        this.interval = (interval == null) ? "MONTHLY" : interval.trim().toUpperCase();
        this.nextChargeDate = nextChargeDate;
        this.billingEmail = (billingEmail == null || billingEmail.isBlank()) ? null : billingEmail.trim().toLowerCase();
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    // ✅ Overload: lar oss lagre cancelUrl/providerKey ved accept
    public Subscription(
            String userEmail,
            String name,
            BigDecimal amount,
            String currency,
            String interval,
            LocalDate nextChargeDate,
            String billingEmail,
            String providerKey,
            String cancelUrl
    ) {
        this(userEmail, name, amount, currency, interval, nextChargeDate, billingEmail);
        this.providerKey = (providerKey == null || providerKey.isBlank()) ? null : providerKey.trim();
        this.cancelUrl = (cancelUrl == null || cancelUrl.isBlank()) ? null : cancelUrl.trim();
    }

    public UUID getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getInterval() { return interval; }
    public LocalDate getNextChargeDate() { return nextChargeDate; }
    public String getBillingEmail() { return billingEmail; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setNextChargeDate(LocalDate nextChargeDate) {
        this.nextChargeDate = nextChargeDate;
    }


    // ✅ NYTT
    public String getCancelUrl() { return cancelUrl; }
    public String getProviderKey() { return providerKey; }

    public void setActive(boolean active) { this.active = active; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    public void setProviderKey(String providerKey) { this.providerKey = providerKey; }

    @Transient
    public BigDecimal getMonthlyCost() {
        if (amount == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        String iv = (interval == null) ? "MONTHLY" : interval.trim().toUpperCase();

        return switch (iv) {
            case "WEEKLY" -> amount.multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case "YEARLY" -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            default -> amount.setScale(2, RoundingMode.HALF_UP);
        };
    }
}
