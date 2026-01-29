package no.hvl.subscriptionapp.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CsvImportAsyncService {

    private final CsvTransactionImportService importer;
    private final CsvImportStatusService status;

    public CsvImportAsyncService(CsvTransactionImportService importer, CsvImportStatusService status) {
        this.importer = importer;
        this.status = status;
    }

    @Async
    public void runImport(String userEmail, Path tempCsvPath) {
        status.set(userEmail, CsvImportStatusService.State.RUNNING, "Importerer CSV…");

        try {
            var res = importer.importCsv(userEmail, tempCsvPath); // vi lager overload under
            status.set(
                    userEmail,
                    CsvImportStatusService.State.DONE,
                    "Ferdig: leste " + res.rowsRead() + ", lagret " + res.inserted() + ", hoppet over " + res.skipped() + "."
            );
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            status.set(userEmail, CsvImportStatusService.State.FAILED, "CSV-import feilet: " + msg);
        } finally {
            try { Files.deleteIfExists(tempCsvPath); } catch (Exception ignored) {}
        }
    }
}
