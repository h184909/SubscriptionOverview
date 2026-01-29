package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.service.CsvTransactionImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class CsvImportController {

    private static final String SESSION_FLASH = "flashMsg";

    private final CsvTransactionImportService csvImport;

    public CsvImportController(CsvTransactionImportService csvImport) {
        this.csvImport = csvImport;
    }

    @GetMapping("/app/transactions/import-csv")
    public String show(HttpSession session, Model model) {
        String flash = (String) session.getAttribute(SESSION_FLASH);
        if (flash != null) session.removeAttribute(SESSION_FLASH);
        model.addAttribute("flashMsg", flash);
        return "import_csv";
    }

    @PostMapping("/app/transactions/import-csv")
    public String upload(HttpSession session, @RequestParam("file") MultipartFile file) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        try {
            var res = csvImport.importCsv(email, file);
            session.setAttribute(SESSION_FLASH,
                    "CSV importert: leste " + res.rowsRead() + ", lagret " + res.inserted() + ", hoppet over " + res.skipped() + "."
            );
            return "redirect:/app/suggestions";
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            session.setAttribute(SESSION_FLASH, "CSV-import feilet: " + msg);
            return "redirect:/app/transactions/import-csv";
        }
    }
}
