package no.hvl.subscriptionapp.service;

import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final PersonRepository personRepo;
    private final MailService mailService;
    private final PasswordService passwordService;

    public PasswordResetService(
            PersonRepository personRepo,
            MailService mailService,
            PasswordService passwordService
    ) {
        this.personRepo = personRepo;
        this.mailService = mailService;
        this.passwordService = passwordService;
    }

    public void issueAndSend(Person p, String baseUrl) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = sha256Hex(token);

        p.setPasswordResetTokenHash(tokenHash);
        p.setPasswordResetExpiresAt(OffsetDateTime.now().plusHours(1));
        personRepo.save(p);

        String link = baseUrl + "/auth/reset-password?token=" + token;

        String subject = "Reset your password";
        String body =
                "Hi!\n\n" +
                        "Click this link to reset your password:\n" +
                        link + "\n\n" +
                        "This link expires in 1 hour.\n\n" +
                        "If you did not request this, you can ignore this email.\n";

        mailService.send(p.getEmail(), subject, body);
    }

    public boolean isValidToken(String token) {
        if (token == null || token.isBlank()) return false;

        String hash = sha256Hex(token.trim());
        Optional<Person> opt = personRepo.findByPasswordResetTokenHash(hash);
        if (opt.isEmpty()) return false;

        Person p = opt.get();
        return p.getPasswordResetExpiresAt() != null
                && OffsetDateTime.now().isBefore(p.getPasswordResetExpiresAt());
    }

    public boolean resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return false;
        }

        String hash = sha256Hex(token.trim());
        Optional<Person> opt = personRepo.findByPasswordResetTokenHash(hash);
        if (opt.isEmpty()) return false;

        Person p = opt.get();
        if (p.getPasswordResetExpiresAt() == null || OffsetDateTime.now().isAfter(p.getPasswordResetExpiresAt())) {
            return false;
        }

        PasswordService.SaltHash sh = passwordService.newSaltHash(newPassword.toCharArray());
        p.setHash(sh.hashHex());
        p.setSalt(sh.saltHex());

        p.setPasswordResetTokenHash(null);
        p.setPasswordResetExpiresAt(null);

        personRepo.save(p);
        return true;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash reset token", e);
        }
    }
}