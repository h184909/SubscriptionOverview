package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpServletRequest;
import no.hvl.subscriptionapp.repository.PersonRepository;
import no.hvl.subscriptionapp.service.EmailVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class EmailVerificationController {

    private final EmailVerificationService verificationService;
    private final PersonRepository personRepository;

    public EmailVerificationController(
            EmailVerificationService verificationService,
            PersonRepository personRepository
    ) {
        this.verificationService = verificationService;
        this.personRepository = personRepository;
    }

    @GetMapping("/verify")
    public String verify(@RequestParam("token") String token, Model model) {
        boolean ok = verificationService.verifyToken(token);
        model.addAttribute("ok", ok);
        return "verify";
    }

    @PostMapping("/resend-verification")
    public String resend(
            @RequestParam("email") String email,
            HttpServletRequest request
    ) {
        return personRepository.findById(email)
                .filter(p -> !p.isEmailVerified())
                .map(p -> {
                    String baseUrl = request.getScheme() + "://" + request.getServerName()
                            + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                            ? ""
                            : ":" + request.getServerPort())
                            + request.getContextPath();

                    verificationService.issueAndSend(p, baseUrl);
                    return "redirect:/login?resent=1";
                })
                .orElse("redirect:/login");
    }
}