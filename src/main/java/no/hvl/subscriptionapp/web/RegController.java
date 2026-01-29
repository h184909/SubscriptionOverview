package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.repository.PersonRepository;
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

    public RegController(PersonRepository personRepository, PasswordService passwordService) {
        this.personRepository = personRepository;
        this.passwordService = passwordService;
    }

    @GetMapping("/register")
    public String showRegister(Model model, HttpSession session) {

        // Logg ut ev. eksisterende bruker før registrering
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
            HttpSession session
    ) {

        // Logg ut ev. eksisterende bruker før registrering (igjen, i tilfelle POST kalles direkte)
        session.removeAttribute(LoginController.SESSION_USER_EMAIL);

        // Passordene må være like
        if (!form.passordErLik()) {
            binding.rejectValue("passordRep", "password.mismatch", "Passordene er ikke like");
        }

        // E-post må være unik
        if (!binding.hasFieldErrors("email") && personRepository.existsById(form.getEmail())) {
            binding.rejectValue("email", "email.exists", "Denne e-posten er allerede registrert");
        }

        if (binding.hasErrors()) {
            return "register";
        }

        PasswordService.SaltHash sh = passwordService.newSaltHash(form.getPassord().toCharArray());
        Person person = new Person(form.getEmail(), sh.hashHex(), sh.saltHex());
        personRepository.save(person);

        session.setAttribute(LoginController.SESSION_USER_EMAIL, person.getEmail());
        return "redirect:/app";
    }
}
