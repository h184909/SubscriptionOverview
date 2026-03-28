package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpServletRequest;
import no.hvl.subscriptionapp.repository.PersonRepository;
import no.hvl.subscriptionapp.service.PasswordResetService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {

    private final PersonRepository personRepository;
    private final PasswordResetService passwordResetService;
    private final MessageSource messageSource;

    public PasswordResetController(
            PersonRepository personRepository,
            PasswordResetService passwordResetService,
            MessageSource messageSource
    ) {
        this.personRepository = personRepository;
        this.passwordResetService = passwordResetService;
        this.messageSource = messageSource;
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot_password";
    }

    @PostMapping("/forgot-password")
    public String doForgotPassword(
            @RequestParam("email") String email,
            HttpServletRequest request,
            Model model,
            Locale locale
    ) {
        if (email != null && !email.isBlank()) {
            personRepository.findById(email.trim()).ifPresent(p -> {
                String baseUrl = "https://" + request.getServerName() + request.getContextPath();
                passwordResetService.issueAndSend(p, baseUrl);
            });
        }

        model.addAttribute(
                "flashMsg",
                messageSource.getMessage("reset.sentIfExists", null, locale)
        );
        return "forgot_password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(
            @RequestParam("token") String token,
            Model model
    ) {
        boolean valid = passwordResetService.isValidToken(token);
        model.addAttribute("token", token);
        model.addAttribute("valid", valid);
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String doResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("password2") String password2,
            Model model,
            Locale locale
    ) {
        model.addAttribute("token", token);

        if (!passwordResetService.isValidToken(token)) {
            model.addAttribute("valid", false);
            model.addAttribute(
                    "flashError",
                    messageSource.getMessage("reset.invalidToken", null, locale)
            );
            return "reset_password";
        }

        model.addAttribute("valid", true);

        if (password == null || password.isBlank() || password2 == null || password2.isBlank()) {
            model.addAttribute(
                    "flashError",
                    messageSource.getMessage("reset.fillAll", null, locale)
            );
            return "reset_password";
        }

        if (!password.equals(password2)) {
            model.addAttribute(
                    "flashError",
                    messageSource.getMessage("reset.passwordsMismatch", null, locale)
            );
            return "reset_password";
        }

        if (password.length() < 8) {
            model.addAttribute(
                    "flashError",
                    messageSource.getMessage("reset.passwordTooShort", null, locale)
            );
            return "reset_password";
        }

        boolean ok = passwordResetService.resetPassword(token, password);
        if (!ok) {
            model.addAttribute("valid", false);
            model.addAttribute(
                    "flashError",
                    messageSource.getMessage("reset.invalidToken", null, locale)
            );
            return "reset_password";
        }

        return "redirect:/login?reset=1";
    }
}