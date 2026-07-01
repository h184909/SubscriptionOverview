package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.repository.PersonRepository;
import no.hvl.subscriptionapp.service.LunchFlowSyncService;
import no.hvl.subscriptionapp.service.PasswordService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.time.Duration;
import java.util.Locale;

@Controller
public class LoginController {

    public static final String SESSION_USER_EMAIL = "userEmail";

    private final PersonRepository personRepository;
    private final PasswordService passwordService;
    private final MessageSource messageSource;
    private final LunchFlowSyncService lunchFlowSyncService;

    public LoginController(
            PersonRepository personRepository,
            PasswordService passwordService,
            MessageSource messageSource,
            LunchFlowSyncService lunchFlowSyncService
    ) {
        this.personRepository = personRepository;
        this.passwordService = passwordService;
        this.messageSource = messageSource;
        this.lunchFlowSyncService = lunchFlowSyncService;
    }

    @GetMapping("/login")
    public String showLogin(
            Model model,
            Locale locale,
            @RequestParam(value = "verify", required = false) String verify,
            @RequestParam(value = "resent", required = false) String resent,
            @RequestParam(value = "reset", required = false) String reset
    ) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }

        if (verify != null) {
            model.addAttribute(
                    "flashMsg",
                    messageSource.getMessage("login.verifySent", null, locale)
            );
        }

        if (resent != null) {
            model.addAttribute(
                    "flashMsg",
                    messageSource.getMessage("login.verifyResent", null, locale)
            );
        }

        if (reset != null) {
            model.addAttribute(
                    "flashMsg",
                    messageSource.getMessage("reset.success", null, locale)
            );
        }

        return "login";
    }

    @PostMapping("/login")
    public String doLogin(
            @Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            Locale locale
    ) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        Person person = personRepository.findById(form.getEmail()).orElse(null);

        if (person == null || !passwordService.verify(form.getPassord(), person.getSalt(), person.getHash())) {
            model.addAttribute(
                    "loginError",
                    messageSource.getMessage("login.invalid", null, locale)
            );
            return "login";
        }

        if (!person.isEmailVerified()) {
            model.addAttribute(
                    "loginError",
                    messageSource.getMessage("login.notVerified", null, locale)
            );
            model.addAttribute("showResend", true);
            model.addAttribute("email", person.getEmail());
            return "login";
        }

        session.setAttribute(SESSION_USER_EMAIL, person.getEmail());

        String pl = person.getPreferredLanguage();
        if (pl != null && !pl.isBlank()) {
            Locale userLocale = "nb".equalsIgnoreCase(pl)
                    ? Locale.forLanguageTag("nb-NO")
                    : Locale.forLanguageTag("en-US");

            session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, userLocale);
        }

        try {
            lunchFlowSyncService.syncIfDue(person.getEmail(), Duration.ofHours(1));
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute(
                    "flashMsg",
                    "Automatic bank sync failed, but you are logged in. You can sync manually from the dashboard."
            );
        }

        return "redirect:/app";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}