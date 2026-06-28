package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.LunchFlowConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LunchFlowConnectionRepository extends JpaRepository<LunchFlowConnection, UUID> {

    Optional<LunchFlowConnection> findFirstByUserEmailOrderByUpdatedAtDesc(String userEmail);

    boolean existsByUserEmail(String userEmail);

    void deleteByUserEmail(String userEmail);
}