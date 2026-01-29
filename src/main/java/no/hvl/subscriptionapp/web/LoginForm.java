package no.hvl.subscriptionapp.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginForm {

    @Email(message = "Ugyldig e-postadresse")
    @NotBlank(message = "E-post er obligatorisk")
    private String email;

    @NotBlank(message = "Passord er obligatorisk")
    @Size(min = 8, message = "Passord må være minst 8 tegn")
    private String passord;

    public LoginForm() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassord() { return passord; }
    public void setPassord(String passord) { this.passord = passord; }}