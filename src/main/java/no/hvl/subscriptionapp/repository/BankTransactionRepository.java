package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    List<BankTransaction> findByUserEmailAndTxDateAfterOrderByTxDateAsc(
            String userEmail,
            OffsetDateTime after
    );

    List<BankTransaction> findByUserEmailOrderByTxDateDesc(
            String userEmail
    );

    boolean existsByUserEmailAndTxId(String userEmail, String txId);

    boolean existsByUserEmailAndAccountIdAndTxId(
            String userEmail,
            String accountId,
            String txId
    );

    List<BankTransaction> findByUserEmailAndAccountIdAndTxIdIn(
            String userEmail,
            String accountId,
            Collection<String> txIds
    );

    void deleteByUserEmail(String userEmail);
}
