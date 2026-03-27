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
        // logg ut ev. eksisterende bruker før registrering
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
        // logg ut ev. eksisterende bruker før registrering
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

        // bygg baseUrl for verifiseringslink
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                ? ""
                : ":" + request.getServerPort())
                + request.getContextPath();

        // send verifiseringsmail
        emailVerificationService.issueAndSend(person, baseUrl);

        // ikke logg inn automatisk - bruker må verifisere først
        return "redirect:/login?verify=1";
    }
}