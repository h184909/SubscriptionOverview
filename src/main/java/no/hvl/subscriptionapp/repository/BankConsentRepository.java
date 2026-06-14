package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.BankConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface BankConsentRepository extends JpaRepository<BankConsent, UUID> {

    Optional<BankConsent> findTopByUserEmailOrderByCreatedAtDesc(String userEmail);

    @Transactional
    @Modifying
    void deleteByUserEmail(String userEmail);
}