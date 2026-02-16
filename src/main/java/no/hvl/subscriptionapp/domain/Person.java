package no.hvl.subscriptionapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(schema = "subscription_app", name = "app_user")
public class Person {

    @Id
    @Email(message = "Ugyldig e-postadresse")
    @NotBlank(message = "E-post er obligatorisk")
    @Size(max = 255, message = "E-post kan ikke være lengre enn 255 tegn")
    @Column(length = 255, nullable = false)
    private String email;

    @Column(length = 64, nullable = false)
    private String hash;

    @Column(length = 32, nullable = false)
    private String salt;

    // ✅ NYTT: lagrer språkvalg per bruker (f.eks "en" eller "nb")
    @Column(length = 8)
    private String preferredLanguage;

    protected Person() {
        // JPA
    }

    public Person(String email, String hash, String salt) {
        this.email = email;
        this.hash = hash;
        this.salt = salt;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
}