package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.repository.PersonRepository;
import no.hvl.subscriptionapp.service.PasswordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Controller
public class LoginController {

    public static final String SESSION_USER_EMAIL = "userEmail";

    private final PersonRepository personRepository;
    private final PasswordService passwordService;

    public LoginController(PersonRepository personRepository, PasswordService passwordService) {
        this.personRepository = personRepository;
        this.passwordService = passwordService;
    }

    @GetMapping("/login")
    public String showLogin(
            Model model,
            @RequestParam(value = "verify", required = false) String verify,
            @RequestParam(value = "resent", required = false) String resent
    ) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }

        if (verify != null) {
            model.addAttribute("flashMsg", "Vi har sendt deg en verifiseringsmail. Sjekk innboksen din.");
        }

        if (resent != null) {
            model.addAttribute("flashMsg", "Ny verifiseringsmail er sendt.");
        }

        return "login";
    }

    @PostMapping("/login")
    public String doLogin(
            @Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        Person person = personRepository.findById(form.getEmail()).orElse(null);

        if (person == null || !passwordService.verify(form.getPassord(), person.getSalt(), person.getHash())) {
            model.addAttribute("loginError", "Feil e-post eller passord");
            return "login";
        }

        if (!person.isEmailVerified()) {
            model.addAttribute("loginError", "Du må verifisere e-posten din før du kan logge inn.");
            model.addAttribute("showResend", true);
            model.addAttribute("email", person.getEmail());
            return "login";
        }

        session.setAttribute(SESSION_USER_EMAIL, person.getEmail());

        // språkpreferanse
        String pl = person.getPreferredLanguage();
        if (pl != null && !pl.isBlank()) {
            Locale locale = "nb".equalsIgnoreCase(pl)
                    ? Locale.forLanguageTag("nb-NO")
                    : Locale.forLanguageTag("en-US");

            session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
        }

        return "redirect:/app";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}