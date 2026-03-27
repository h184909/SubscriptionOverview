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
public class EmailVerificationService {

    private final PersonRepository personRepo;
    private final MailService mailService;

    public EmailVerificationService(PersonRepository personRepo, MailService mailService) {
        this.personRepo = personRepo;
        this.mailService = mailService;
    }

    public void issueAndSend(Person p, String baseUrl) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = sha256Hex(token);

        p.setEmailVerifyTokenHash(tokenHash);
        p.setEmailVerifyExpiresAt(OffsetDateTime.now().plusHours(24));
        p.setEmailVerified(false);
        personRepo.save(p);

        String link = baseUrl + "/auth/verify?token=" + token;

        String subject = "Verify your email";
        String body =
                "Hi!\n\n" +
                        "Please verify your email by clicking this link:\n" +
                        link + "\n\n" +
                        "This link expires in 24 hours.\n\n" +
                        "If you did not create an account, you can ignore this email.\n";

        mailService.send(p.getEmail(), subject, body);
    }

    public boolean verifyToken(String token) {
        if (token == null || token.isBlank()) return false;

        String hash = sha256Hex(token.trim());
        Optional<Person> opt = personRepo.findByEmailVerifyTokenHash(hash);
        if (opt.isEmpty()) return false;

        Person p = opt.get();

        if (p.isEmailVerified()) return true;

        if (p.getEmailVerifyExpiresAt() == null || OffsetDateTime.now().isAfter(p.getEmailVerifyExpiresAt())) {
            return false;
        }

        p.setEmailVerified(true);
        p.setEmailVerifyTokenHash(null);
        p.setEmailVerifyExpiresAt(null);
        personRepo.save(p);

        return true;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash verification token", e);
        }
    }
}