package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.BankConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankConsentRepository extends JpaRepository<BankConsent, UUID> {
    Optional<BankConsent> findTopByUserEmailOrderByCreatedAtDesc(String userEmail);
    void deleteByUserEmail(String userEmail);


}
