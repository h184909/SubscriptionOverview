package no.hvl.subscriptionapp.service;

import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.domain.LunchFlowConnection;
import no.hvl.subscriptionapp.openbanking.lunchflow.LunchFlowDtos;
import no.hvl.subscriptionapp.openbanking.lunchflow.LunchFlowHttp;
import no.hvl.subscriptionapp.openbanking.lunchflow.LunchFlowProperties;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import no.hvl.subscriptionapp.repository.LunchFlowConnectionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LunchFlowSyncService {

    private final LunchFlowHttp lunchFlow;
    private final LunchFlowProperties props;
    private final BankTransactionRepository txRepo;
    private final LunchFlowConnectionRepository connectionRepo;

    public LunchFlowSyncService(
            LunchFlowHttp lunchFlow,
            LunchFlowProperties props,
            BankTransactionRepository txRepo,
            LunchFlowConnectionRepository connectionRepo
    ) {
        this.lunchFlow = lunchFlow;
        this.props = props;
        this.txRepo = txRepo;
        this.connectionRepo = connectionRepo;
    }

    public ImportResult syncNow(String userEmail) {
        LunchFlowConnection connection = connectionRepo
                .findFirstByUserEmailOrderByUpdatedAtDesc(userEmail)
                .orElseThrow(() -> new IllegalStateException("No bank connection found. Connect your bank first."));

        try {
            return importTransactions(userEmail, connection, connection.getAccessToken());
        } catch (Exception firstError) {
            LunchFlowDtos.TokenRequest refreshRequest = new LunchFlowDtos.TokenRequest(
                    "refresh_token",
                    null,
                    props.getRedirectUri(),
                    props.getClientId(),
                    props.getClientSecret(),
                    connection.getRefreshToken()
            );

            LunchFlowDtos.TokenResponse refreshed = lunchFlow.exchangeCode(refreshRequest);

            connection.updateTokens(
                    refreshed.user_id(),
                    refreshed.access_token(),
                    refreshed.refresh_token()
            );
            connectionRepo.save(connection);

            return importTransactions(userEmail, connection, refreshed.access_token());
        }
    }

    public void syncIfDue(String userEmail, Duration maxAge) {
        LunchFlowConnection connection = connectionRepo
                .findFirstByUserEmailOrderByUpdatedAtDesc(userEmail)
                .orElse(null);

        if (connection == null) return;

        OffsetDateTime last = connection.getLastSyncedAt();
        if (last != null && last.isAfter(OffsetDateTime.now().minus(maxAge))) {
            return;
        }

        syncNow(userEmail);
    }

    private ImportResult importTransactions(String userEmail, LunchFlowConnection connection, String accessToken) {
        LunchFlowDtos.AccountsResponse accountsRes = lunchFlow.getAccounts(accessToken);

        if (accountsRes == null || accountsRes.accounts() == null || accountsRes.accounts().isEmpty()) {
            connection.markSynced(null, 0, null);
            connectionRepo.save(connection);
            return new ImportResult(0, 0, 0);
        }

        int accountsFound = accountsRes.accounts().size();
        int transactionsFound = 0;
        int transactionsImported = 0;

        String institutionName = accountsRes.accounts().stream()
                .map(LunchFlowDtos.Account::institution_name)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse("Lunch Flow");

        String accountNames = accountsRes.accounts().stream()
                .map(LunchFlowDtos.Account::name)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));

        for (LunchFlowDtos.Account account : accountsRes.accounts()) {
            if (account.id() == null || account.id().isBlank()) continue;

            LunchFlowDtos.TransactionsResponse txRes =
                    lunchFlow.getTransactions(accessToken, account.id());

            if (txRes == null || txRes.transactions() == null || txRes.transactions().isEmpty()) continue;

            transactionsFound += txRes.transactions().size();

            Set<String> seenInThisImport = new HashSet<>();

            for (LunchFlowDtos.Transaction tx : txRes.transactions()) {
                String txId = normalizeTxId(account.id(), tx);

                if (txId == null || txId.isBlank()) continue;
                if (!seenInThisImport.add(txId)) continue;

                if (txRepo.existsByUserEmailAndAccountIdAndTxId(userEmail, account.id(), txId)) {
                    continue;
                }

                BankTransaction entity = new BankTransaction(
                        userEmail,
                        account.id(),
                        txId,
                        parseLunchFlowDate(tx.date()),
                        tx.date(),
                        firstNonBlank(tx.description(), tx.merchant()),
                        tx.merchant(),
                        tx.amount(),
                        tx.currency()
                );

                txRepo.save(entity);
                transactionsImported++;
            }
        }

        connection.markSynced(institutionName, accountsFound, accountNames);
        connectionRepo.save(connection);

        return new ImportResult(accountsFound, transactionsFound, transactionsImported);
    }

    private String normalizeTxId(String accountId, LunchFlowDtos.Transaction tx) {
        if (tx == null) return null;

        if (tx.id() != null && !tx.id().isBlank() && !"0".equals(tx.id().trim())) {
            return tx.id().trim();
        }

        return accountId + "|" +
                safe(tx.date()) + "|" +
                safe(tx.amount() == null ? null : tx.amount().toPlainString()) + "|" +
                safe(tx.description()) + "|" +
                safe(tx.merchant());
    }

    private OffsetDateTime parseLunchFlowDate(String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception ignored) {}

        try {
            return LocalDate.parse(raw).atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {}

        return null;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public record ImportResult(
            int accountsFound,
            int transactionsFound,
            int transactionsImported
    ) {}
}