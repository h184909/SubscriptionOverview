package no.hvl.subscriptionapp.openbanking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representerer en bankkonto fra Yapily
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {

    private String id;
    private String nickname;
    private String currency;

    // --- Tom konstruktør (påkrevd av Jackson) ---
    public Account() {
    }

    // --- Getters / setters (JSP EL + Jackson-vennlig) ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
