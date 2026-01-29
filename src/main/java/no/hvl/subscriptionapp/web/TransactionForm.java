package no.hvl.subscriptionapp.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransactionForm {

    @NotBlank(message = "Dato er påkrevd (yyyy-MM-dd)")
    private String bookedDate;

    @NotBlank(message = "Beskrivelse er påkrevd")
    @Size(max = 200, message = "Maks 200 tegn")
    private String description;

    @NotNull(message = "Beløp er påkrevd")
    @DecimalMin(value = "0.01", message = "Beløp må være > 0")
    private BigDecimal amount;

    @NotBlank(message = "Valuta er påkrevd")
    @Size(min = 3, max = 3, message = "Valuta må være 3 bokstaver (NOK)")
    private String currency = "NOK";

    public String getBookedDate() { return bookedDate; }
    public void setBookedDate(String bookedDate) { this.bookedDate = bookedDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
