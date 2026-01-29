package no.hvl.subscriptionapp.service;

import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CsvTransactionImportService {

    private final BankTransactionRepository txRepo;
    private final TransactionTemplate tx;

    public CsvTransactionImportService(
            BankTransactionRepository txRepo,
            PlatformTransactionManager txManager
    ) {
        this.txRepo = txRepo;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setTimeout(20);
    }

    public record ImportResult(int rowsRead, int inserted, int skipped) {}

    private static final String ACCOUNT_ID = "CSV";
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ISO_LOCAL_DATE;

    private record RowParsed(LocalDate booked, String desc, BigDecimal amount, String currency, String txId) {}

    public ImportResult importCsv(String userEmail, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Filen er tom.");
        try (InputStream is = file.getInputStream()) {
            return importCsv(userEmail, is);
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke lese CSV-filen.", e);
        }
    }

    public ImportResult importCsv(String userEmail, Path path) {
        try (InputStream is = java.nio.file.Files.newInputStream(path)) {
            return importCsv(userEmail, is);
        } catch (Exception e) {
            throw new IllegalStateException("Kunne ikke lese tempfil for CSV-import.", e);
        }
    }

    private ImportResult importCsv(String userEmail, InputStream is) {
        int rowsRead = 0;
        int inserted = 0;
        int skipped = 0;

        final int BATCH_SIZE = 150; // kortere transaksjoner => færre leaks/timeouts
        List<RowParsed> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String headerLine = br.readLine();
            if (headerLine == null) throw new IllegalArgumentException("CSV mangler header-linje.");
            headerLine = stripBom(headerLine);

            char sep = guessSeparator(headerLine);
            String[] headers = parseCsvLine(headerLine, sep);
            Map<String, Integer> idx = buildIndex(headers);

            int iDate = requireAny(idx,
                    "date", "dato", "bokført", "bokfort", "bokføringsdato", "transaksjonsdato", "posted date", "booking date"
            );
            int iDesc = optionalAny(idx,
                    "description", "beskrivelse", "tekst", "mottaker", "tittel", "transaksjonstekst", "merchant", "name"
            );
            int iMsg  = optionalAny(idx,
                    "melding", "message", "info", "tilleggsinformasjon", "additional information"
            );
            int iOut  = optionalAny(idx,
                    "ut av konto", "ut", "debet", "debit", "beløp ut", "belop ut", "withdrawal"
            );
            int iIn   = optionalAny(idx,
                    "inn på konto", "inn", "kredit", "credit", "beløp inn", "belop inn", "deposit"
            );
            int iAmt  = optionalAny(idx,
                    "amount", "beløp", "belop", "sum", "total"
            );
            int iCur  = optionalAny(idx,
                    "currency", "valuta", "valutasort", "ccy"
            );

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                rowsRead++;

                String[] row = parseCsvLine(line, sep);

                LocalDate booked = parseDate(get(row, iDate));
                if (booked == null) { skipped++; continue; }

                String desc = firstNonBlank(get(row, iDesc), get(row, iMsg), "");
                desc = trimToLen(desc, 500);
                if (desc.isBlank()) desc = "Ukjent";

                String currency = firstNonBlank(get(row, iCur), "NOK").trim().toUpperCase(Locale.ROOT);
                currency = currency.replaceAll("[^A-Z]", "");
                if (currency.length() > 3) currency = currency.substring(0, 3);
                if (currency.isBlank()) currency = "NOK";

                BigDecimal amount = parseAmount(get(row, iAmt));
                if (amount == null) {
                    BigDecimal out = parseAmount(get(row, iOut));
                    BigDecimal inn = parseAmount(get(row, iIn));

                    if (out != null && out.compareTo(BigDecimal.ZERO) != 0) amount = out;
                    else if (inn != null && inn.compareTo(BigDecimal.ZERO) != 0) amount = inn;
                    else { skipped++; continue; }
                }
                amount = amount.setScale(2, RoundingMode.HALF_UP);

                String txId = "csv_" + sha256(booked + "|" + desc + "|" + amount + "|" + currency);

                batch.add(new RowParsed(booked, desc, amount, currency, txId));

                if (batch.size() >= BATCH_SIZE) {
                    int[] r = persistBatch(userEmail, batch);
                    inserted += r[0];
                    skipped  += r[1];
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                int[] r = persistBatch(userEmail, batch);
                inserted += r[0];
                skipped  += r[1];
                batch.clear();
            }

            return new ImportResult(rowsRead, inserted, skipped);

        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke lese CSV-filen.", e);
        }
    }

    private int[] persistBatch(String userEmail, List<RowParsed> batch) {
        // Korte transaksjoner, commit ofte
        return tx.execute(status -> {

            // 1) dedupe i batch
            Map<String, RowParsed> unique = new LinkedHashMap<>();
            int skippedBatchDupes = 0;
            for (RowParsed rp : batch) {
                if (unique.putIfAbsent(rp.txId, rp) != null) skippedBatchDupes++;
            }
            if (unique.isEmpty()) return new int[]{0, skippedBatchDupes};

            // 2) sjekk hvilke finnes i DB allerede (1 query)
            Set<String> ids = unique.keySet();
            Set<String> existing = new HashSet<>();
            var existingRows = txRepo.findByUserEmailAndAccountIdAndTxIdIn(userEmail, ACCOUNT_ID, ids);
            for (var t : existingRows) existing.add(t.getTxId());

            // 3) bygg insert-liste
            List<BankTransaction> toInsert = new ArrayList<>(unique.size());
            int skippedExisting = 0;

            for (RowParsed rp : unique.values()) {
                if (existing.contains(rp.txId)) { skippedExisting++; continue; }

                OffsetDateTime txDate = rp.booked.atStartOfDay().atOffset(ZoneOffset.UTC);

                toInsert.add(new BankTransaction(
                        userEmail,
                        ACCOUNT_ID,
                        rp.txId,
                        txDate,
                        rp.booked.toString(),
                        rp.desc,
                        "",
                        rp.amount,
                        rp.currency
                ));
            }

            int inserted = 0;
            int skippedRace = 0;

            // 4) batch insert (med fallback)
            try {
                if (!toInsert.isEmpty()) {
                    txRepo.saveAllAndFlush(toInsert);
                    inserted = toInsert.size();
                }
            } catch (DataIntegrityViolationException ex) {
                inserted = 0;
                for (BankTransaction bt : toInsert) {
                    try {
                        txRepo.saveAndFlush(bt);
                        inserted++;
                    } catch (DataIntegrityViolationException ignored) {
                        skippedRace++;
                    }
                }
            }

            int skippedTotal = skippedBatchDupes + skippedExisting + skippedRace;
            return new int[]{inserted, skippedTotal};
        });
    }

    // ----------------- CSV helpers -----------------

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') return s.substring(1);
        return s;
    }

    private static char guessSeparator(String headerLine) {
        long semi = headerLine.chars().filter(c -> c == ';').count();
        long comma = headerLine.chars().filter(c -> c == ',').count();
        return semi >= comma ? ';' : ',';
    }

    private static String[] parseCsvLine(String line, char sep) {
        if (line == null) return new String[0];

        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (ch == sep && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
                continue;
            }

            cur.append(ch);
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private static Map<String, Integer> buildIndex(String[] headers) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String k = normHeader(headers[i]);
            if (!k.isBlank()) m.put(k, i);
        }
        return m;
    }

    private static int requireAny(Map<String, Integer> idx, String... names) {
        Integer i = any(idx, names);
        if (i == null) throw new IllegalArgumentException("CSV mangler kolonne: " + names[0] + ". Fant: " + idx.keySet());
        return i;
    }

    private static int optionalAny(Map<String, Integer> idx, String... names) {
        Integer i = any(idx, names);
        return i == null ? -1 : i;
    }

    private static Integer any(Map<String, Integer> idx, String... names) {
        for (String n : names) {
            Integer i = idx.get(normHeader(n));
            if (i != null) return i;
        }
        return null;
    }

    private static String get(String[] row, int i) {
        if (i < 0 || row == null || i >= row.length) return "";
        return row[i] == null ? "" : row[i].trim();
    }

    private static String normHeader(String s) {
        if (s == null) return "";
        String t = s.trim().replace("\uFEFF", "");
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) t = t.substring(1, t.length() - 1);
        t = t.trim().toLowerCase(Locale.ROOT);
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x;
        return "";
    }

    private static String trimToLen(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static LocalDate parseDate(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isBlank()) return null;
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) t = t.substring(1, t.length() - 1);

        try { return LocalDate.parse(t, DMY); } catch (Exception ignored) {}
        try { return LocalDate.parse(t, YMD); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(t).toLocalDate(); } catch (Exception ignored) {}
        return null;
    }

    private static BigDecimal parseAmount(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isBlank()) return null;
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) t = t.substring(1, t.length() - 1);

        t = t.replace(" ", "").replace("kr", "").replace("KR", "");

        int lastComma = t.lastIndexOf(',');
        int lastDot = t.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                t = t.replace(".", "");
                t = t.replace(",", ".");
            } else {
                t = t.replace(",", "");
            }
        } else if (lastComma >= 0) {
            t = t.replace(".", "");
            t = t.replace(",", ".");
        } else {
            t = t.replace(",", "");
        }

        t = t.replaceAll("[^0-9\\-\\.]", "");

        try { return new BigDecimal(t); } catch (Exception e) { return null; }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) sb.append(String.format("%02x", dig[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(s));
        }
    }
}
