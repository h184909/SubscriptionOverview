package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.repository.PersonRepository;
import no.hvl.subscriptionapp.service.EmailVerificationService;
import no.hvl.subscriptionapp.service.PasswordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class RegController {

    private final PersonRepository personRepository;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;

    public RegController(
            PersonRepository personRepository,
            PasswordService passwordService,
            EmailVerificationService emailVerificationService
    ) {
        this.personRepository = personRepository;
        this.passwordService = passwordService;
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/register")
    public String showRegister(Model model, HttpSession session) {
        session.removeAttribute(LoginController.SESSION_USER_EMAIL);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult binding,
            Model model,
            HttpSession session,
            HttpServletRequest request
    ) {
        session.removeAttribute(LoginController.SESSION_USER_EMAIL);

        if (!form.passordErLik()) {
            binding.rejectValue("passordRep", "password.mismatch", "Passordene er ikke like");
        }

        if (!binding.hasFieldErrors("email") && personRepository.existsById(form.getEmail())) {
            binding.rejectValue("email", "email.exists", "Denne e-posten er allerede registrert");
        }

        if (binding.hasErrors()) {
            return "register";
        }

        PasswordService.SaltHash sh = passwordService.newSaltHash(form.getPassord().toCharArray());

        Person person = new Person(form.getEmail(), sh.hashHex(), sh.saltHex());
        person.setEmailVerified(false);
        personRepository.save(person);

        try {
            // ✅ alltid bygg verify-link med HTTPS
            String baseUrl = "https://" + request.getServerName() + request.getContextPath();

            emailVerificationService.issueAndSend(person, baseUrl);

            return "redirect:/login?verify=1";
        } catch (Exception e) {
            // hvis e-postsending feiler, fjern brukeren igjen så den ikke blir stående halvferdig
            personRepository.deleteById(person.getEmail());

            model.addAttribute("feilmeldinger", java.util.List.of(
                    "Could not send verification email right now. Please try again."
            ));
            return "register";
        }
    }
}