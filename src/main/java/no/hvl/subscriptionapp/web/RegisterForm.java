package no.hvl.subscriptionapp.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @Email(message = "Ugyldig e-postadresse")
    @NotBlank(message = "E-post er obligatorisk")
    @Size(max = 255, message = "E-post kan ikke være lengre enn 255 tegn")
    private String email;

    @NotBlank(message = "Passord er obligatorisk")
    @Size(min = 8, message = "Passord må være minst 8 tegn")
    private String passord;

    @NotBlank(message = "Gjenta passord er obligatorisk")
    private String passordRep;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassord() { return passord; }
    public void setPassord(String passord) { this.passord = passord; }

    public String getPassordRep() { return passordRep; }
    public void setPassordRep(String passordRep) { this.passordRep = passordRep; }

    public boolean passordErLik() {
        return passord != null && passord.equals(passordRep);
    }
}
