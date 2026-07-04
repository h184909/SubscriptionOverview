package no.hvl.subscriptionapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Forslag til abonnement basert på banktransaksjoner
 */
public class SubscriptionSuggestion {

    private final String key;
    private final String name;
    private final BigDecimal amount;
    private final String currency;
    private final String interval;
    private final LocalDate lastChargeDate;
    private final LocalDate nextExpectedDate;
    private final int occurrences;
    private final int confidence;
    private final boolean knownProvider;
    private final String providerKey;
    private final String cancelUrl;
    private final String category;

    public SubscriptionSuggestion(
            String key,
            String name,
            BigDecimal amount,
            String currency,
            String interval,
            LocalDate lastChargeDate,
            LocalDate nextExpectedDate,
            int occurrences,
            int confidence,
            boolean knownProvider,
            String providerKey,
            String cancelUrl,
            String category
    ) {
        this.key = key;
        this.name = name;
        this.amount = amount;
        this.currency = currency;
        this.interval = interval;
        this.lastChargeDate = lastChargeDate;
        this.nextExpectedDate = nextExpectedDate;
        this.occurrences = occurrences;
        this.confidence = confidence;
        this.knownProvider = knownProvider;
        this.providerKey = providerKey;
        this.cancelUrl = cancelUrl;
        this.category = category;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getInterval() { return interval; }
    public LocalDate getLastChargeDate() { return lastChargeDate; }
    public LocalDate getNextExpectedDate() { return nextExpectedDate; }
    public int getOccurrences() { return occurrences; }
    public int getConfidence() { return confidence; }
    public boolean isKnownProvider() { return knownProvider; }
    public String getProviderKey() { return providerKey; }
    public String getCancelUrl() { return cancelUrl; }
    public String getCategory() { return category; }
}
