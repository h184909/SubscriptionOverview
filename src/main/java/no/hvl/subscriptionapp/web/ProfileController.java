package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.LunchFlowConnection;
import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.repository.*;
import no.hvl.subscriptionapp.service.PasswordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProfileController {

    private final PersonRepository personRepo;
    private final PasswordService passwordService;
    private final SubscriptionRepository subscriptionRepo;
    private final BankTransactionRepository txRepo;
    private final BankConsentRepository consentRepo;
    private final SuggestionDecisionRepository decisionRepo;
    private final LunchFlowConnectionRepository lunchFlowConnectionRepo;

    public ProfileController(
            PersonRepository personRepo,
            PasswordService passwordService,
            SubscriptionRepository subscriptionRepo,
            BankTransactionRepository txRepo,
            BankConsentRepository consentRepo,
            SuggestionDecisionRepository decisionRepo,
            LunchFlowConnectionRepository lunchFlowConnectionRepo
    ) {
        this.personRepo = personRepo;
        this.passwordService = passwordService;
        this.subscriptionRepo = subscriptionRepo;
        this.txRepo = txRepo;
        this.consentRepo = consentRepo;
        this.decisionRepo = decisionRepo;
        this.lunchFlowConnectionRepo = lunchFlowConnectionRepo;
    }

    @GetMapping("/app/profile")
    public String profile(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        Person p = personRepo.findById(email).orElse(null);
        if (p == null) return "redirect:/login";

        model.addAttribute("email", p.getEmail());
        model.addAttribute("preferredLanguage", p.getPreferredLanguage());

        LunchFlowConnection connection =
                lunchFlowConnectionRepo.findFirstByUserEmailOrderByUpdatedAtDesc(email).orElse(null);

        model.addAttribute("bankConnected", connection != null);
        model.addAttribute("bankConsent", connection);

        return "profile";
    }

    @PostMapping("/app/profile/change-password")
    public String changePassword(
            HttpSession session,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("newPassword2") String newPassword2,
            Model model
    ) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        Person p = personRepo.findById(email).orElse(null);
        if (p == null) return "redirect:/login";

        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || newPassword2 == null || newPassword2.isBlank()) {
            model.addAttribute("flashError", "Please fill in all fields.");
            return profile(session, model);
        }

        if (!newPassword.equals(newPassword2)) {
            model.addAttribute("flashError", "New passwords do not match.");
            return profile(session, model);
        }

        if (newPassword.length() < 8) {
            model.addAttribute("flashError", "New password must be at least 8 characters.");
            return profile(session, model);
        }

        if (!passwordService.verify(currentPassword, p.getSalt(), p.getHash())) {
            model.addAttribute("flashError", "Wrong current password.");
            return profile(session, model);
        }

        var sh = passwordService.newSaltHash(newPassword.toCharArray());
        p.setHash(sh.hashHex());
        p.setSalt(sh.saltHex());
        personRepo.save(p);

        model.addAttribute("flashMsg", "Password updated.");
        return profile(session, model);
    }

    @PostMapping("/app/profile/disconnect-bank")
    public String disconnectBank(HttpSession session) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        lunchFlowConnectionRepo.deleteByUserEmail(email);
        consentRepo.deleteByUserEmail(email);

        session.setAttribute("flashMsg", "Bank disconnected.");
        return "redirect:/app/profile";
    }

    @PostMapping("/app/profile/delete")
    public String deleteAccount(
            HttpSession session,
            @RequestParam("password") String password,
            @RequestParam("confirm") String confirm,
            Model model
    ) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        Person p = personRepo.findById(email).orElse(null);
        if (p == null) return "redirect:/login";

        if (password == null || password.isBlank()) {
            model.addAttribute("flashError", "Password is required to delete your account.");
            return profile(session, model);
        }

        if (!"DELETE".equals(confirm)) {
            model.addAttribute("flashError", "Type DELETE to confirm account deletion.");
            return profile(session, model);
        }

        if (!passwordService.verify(password, p.getSalt(), p.getHash())) {
            model.addAttribute("flashError", "Wrong password.");
            return profile(session, model);
        }

        try { decisionRepo.deleteByUserEmail(email); } catch (Exception ignored) {}
        try { subscriptionRepo.deleteByUserEmail(email); } catch (Exception ignored) {}
        try { txRepo.deleteByUserEmail(email); } catch (Exception ignored) {}
        try { consentRepo.deleteByUserEmail(email); } catch (Exception ignored) {}
        try { lunchFlowConnectionRepo.deleteByUserEmail(email); } catch (Exception ignored) {}

        personRepo.deleteById(email);
        session.invalidate();

        return "redirect:/";
    }

    @PostMapping("/app/profile/language")
    public String updateLanguage(
            HttpSession session,
            @RequestParam("lang") String lang
    ) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        String v = (lang == null ? "" : lang.trim().toLowerCase());

        var locale = switch (v) {
            case "nb", "no" -> java.util.Locale.forLanguageTag("nb-NO");
            default -> java.util.Locale.forLanguageTag("en-US");
        };

        session.setAttribute(
                org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                locale
        );

        personRepo.findById(email).ifPresent(p -> {
            p.setPreferredLanguage(locale.getLanguage());
            personRepo.save(p);
        });

        return "redirect:/app/profile";
    }
}