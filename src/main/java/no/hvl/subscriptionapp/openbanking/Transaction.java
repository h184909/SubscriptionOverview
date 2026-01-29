package no.hvl.subscriptionapp.openbanking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representerer en transaksjon fra Yapily (JSP EL + Jackson-vennlig)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    private String id;
    private String date;        // Yapily returnerer ofte date/time som string
    private String description;
    private String reference;

    // Beløp kan være strukturert i Yapily (amount + currency), men vi tar en enkel variant først:
    private Double amount;
    private String currency;

    public Transaction() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
