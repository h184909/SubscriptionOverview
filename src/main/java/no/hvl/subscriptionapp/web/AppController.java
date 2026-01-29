package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.BankConsentRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.service.ExchangeRateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class AppController {

    private final SubscriptionRepository subscriptionRepo;
    private final BankConsentRepository consentRepo;
    private final ExchangeRateService fx;

    public AppController(
            SubscriptionRepository subscriptionRepo,
            BankConsentRepository consentRepo,
            ExchangeRateService fx
    ) {
        this.subscriptionRepo = subscriptionRepo;
        this.consentRepo = consentRepo;
        this.fx = fx;
    }

    @GetMapping("/app")
    public String dashboard(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        model.addAttribute("email", email);

        // Bank-tilkobling (til UI)
        boolean bankConnected = consentRepo.findTopByUserEmailOrderByCreatedAtDesc(email).isPresent();
        model.addAttribute("bankConnected", bankConnected);

        // Subs (bruker din repo-metode)
        List<Subscription> subs = subscriptionRepo.findByUserEmailOrderByCreatedAtDesc(email);
        model.addAttribute("subs", subs);

        // TODO: behold/legg inn din dueSoon-logikk her om du har den fra før
        // model.addAttribute("dueSoon", ...);

        // Total per måned i NOK (konverter alle valutaer -> NOK)
        BigDecimal totalMonthlyNok = BigDecimal.ZERO;

        for (Subscription s : subs) {
            BigDecimal monthly = s.getMonthlyCost(); // må finnes hos deg
            if (monthly == null) continue;

            BigDecimal inNok = fx.convertToNok(monthly, s.getCurrency());
            if (inNok != null) {
                totalMonthlyNok = totalMonthlyNok.add(inNok);
            }
        }

        model.addAttribute("totalMonthlyNok", totalMonthlyNok);

        // (valgfritt) hvis du bruker dette i dashboard.jsp
        model.addAttribute("showDevLinks", false);

        return "app";
    }
}
