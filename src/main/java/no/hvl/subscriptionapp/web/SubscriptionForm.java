package no.hvl.subscriptionapp.web;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class SubscriptionForm {

    @NotBlank(message = "Navn er obligatorisk")
    @Size(min = 2, max = 80)
    private String name;

    @NotNull(message = "Pris er obligatorisk")
    @DecimalMin(value = "0.00", inclusive = false, message = "Pris må være større enn 0")
    @Digits(integer = 8, fraction = 2, message = "Maks 2 desimaler")
    private BigDecimal amount; // kroner

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency = "NOK";

    @NotBlank
    @Pattern(regexp = "^(WEEKLY|MONTHLY|YEARLY)$")
    private String interval = "MONTHLY";

    private String nextChargeDate;

    @Email(message = "Ugyldig e-post")
    private String billingEmail;

    public SubscriptionForm() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }

    public String getNextChargeDate() { return nextChargeDate; }
    public void setNextChargeDate(String nextChargeDate) { this.nextChargeDate = nextChargeDate; }

    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String billingEmail) { this.billingEmail = billingEmail; }
}
