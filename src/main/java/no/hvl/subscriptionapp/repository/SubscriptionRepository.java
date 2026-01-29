package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<Subscription> findByUserEmailAndActiveTrueOrderByCreatedAtDesc(String userEmail);

    List<Subscription> findByUserEmail(String userEmail);


    boolean existsByUserEmailAndNameAndIntervalAndAmount(
            String userEmail,
            String name,
            String interval,
            java.math.BigDecimal amount
    );

    boolean existsByUserEmailAndNameAndAmountAndCurrencyAndInterval(
            String userEmail,
            String name,
            java.math.BigDecimal amount,
            String currency,
            String interval
    );



}
